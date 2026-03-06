package org.texttechnologylab.uce.corpusimporter;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.Anomaly;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.annotation.AnnotationComment;
import org.texttechnologylab.annotation.DocumentAnnotation;
import org.texttechnologylab.annotation.Emotion;
import org.texttechnologylab.annotation.SentimentModel;
import org.texttechnologylab.annotation.geonames.GeoNamesEntity;
import org.texttechnologylab.annotation.link.ADLink;
import org.texttechnologylab.annotation.link.DALink;
import org.texttechnologylab.annotation.link.DLink;
import org.texttechnologylab.annotation.ocr.OCRBlock;
import org.texttechnologylab.annotation.ocr.OCRLine;
import org.texttechnologylab.annotation.ocr.OCRPage;
import org.texttechnologylab.annotation.ocr.OCRToken;
import org.texttechnologylab.annotation.uce.Permission;
import org.texttechnologylab.models.authentication.DocumentPermission;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.config.CorpusConfig;
import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.exceptions.DocumentAccessDeniedException;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.biofid.BiofidTaxon;
import org.texttechnologylab.uce.common.models.biofid.GazetteerTaxon;
import org.texttechnologylab.uce.common.models.biofid.GnFinderTaxon;
import org.texttechnologylab.uce.common.models.corpus.*;
import org.texttechnologylab.uce.common.models.corpus.emotion.Feeling;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationLink;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationToDocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentToAnnotationLink;
import org.texttechnologylab.uce.common.models.corpus.ocr.OCRPageAdapterImpl;
import org.texttechnologylab.uce.common.models.corpus.ocr.PageAdapter;
import org.texttechnologylab.uce.common.models.corpus.ocr.PageAdapterImpl;
import org.texttechnologylab.uce.common.models.imp.ImportLog;
import org.texttechnologylab.uce.common.models.imp.ImportStatus;
import org.texttechnologylab.uce.common.models.imp.LogStatus;
import org.texttechnologylab.uce.common.models.negation.*;
import org.texttechnologylab.uce.common.models.rag.DocumentChunkEmbedding;
import org.texttechnologylab.uce.common.models.rag.DocumentSentenceEmbedding;
import org.texttechnologylab.uce.common.models.topic.TopicValueBase;
import org.texttechnologylab.uce.common.models.topic.TopicValueBaseWithScore;
import org.texttechnologylab.uce.common.models.topic.TopicWord;
import org.texttechnologylab.uce.common.models.topic.UnifiedTopic;
import org.texttechnologylab.uce.common.services.*;
import org.texttechnologylab.uce.common.utils.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DocumentImportContinuation {

    private final PostgresqlDataInterface_Impl db;
    private final LexiconService lexiconService;
    private final Logger logger;
    private final AtomicReference<CountDownLatch> batchLatch;
    private final AtomicInteger docInBatch;
    private final Object lock;
    private final int batchSize;
    private final Corpus corpus;
    private final CorpusConfig corpusConfig;
    private String importId;
    private Integer importerNumber;
    private EmbeddingService embeddingService;
    private CommonConfig commonConfig = new CommonConfig();


    public DocumentImportContinuation(
            PostgresqlDataInterface_Impl db,
            LexiconService lexiconService,
            Logger logger,
            AtomicReference<CountDownLatch> batchLatch,
            AtomicInteger docInBatch,
            Object lock,
            int batchSize,
            Corpus corpus,
            CorpusConfig corpusConfig,
            int importerNumber,
            String importId,
            EmbeddingService embeddingService

    ) {
        this.db = db;
        this.lexiconService = lexiconService;
        this.logger = logger;
        this.batchLatch = batchLatch;
        this.docInBatch = docInBatch;
        this.lock = lock;
        this.batchSize = batchSize;
        this.corpus = corpus;
        this.corpusConfig = corpusConfig;
        this.importerNumber = importerNumber;
        this.importId = importId;
        this.embeddingService = embeddingService;
    }

    public Document saveDocument(Document doc, Path filePath) {
        if (doc == null) {
            return null;
        }

        logger.info("Trying to store document with document id " + doc.getDocumentId() + "...");

        ExceptionUtils.tryCatchLog(
                () -> db.saveDocument(doc),
                ex -> logImportError(
                        "Error saving document with id " + doc.getId(),
                        ex,
                        filePath.toString()
                )
        );

        return doc;
    }

    public void postProcessDocumentAndBatch(Document doc, Path filePath) {
        if (doc != null) {
            logImportInfo("Stored document " + filePath.getFileName(), LogStatus.SAVED, filePath.toString(), 0);
            logger.info("Finished with the UIMA annotations - postprocessing the doc now.");

            ExceptionUtils.tryCatchLog(
                    () -> postProccessDocument(doc, corpus, filePath.toString()),
                    ex -> logImportError(
                            "Error postprocessing a saved document with id " + doc.getId(),
                            ex,
                            filePath.toString()
                    )
            );

            logImportInfo("Finished with import.", LogStatus.FINISHED, filePath.toString(), 0);
        }

        int local = docInBatch.incrementAndGet();
        if (local == batchSize) {
            synchronized (lock) {
                if (docInBatch.get() == batchSize) {
                    docInBatch.set(0);
                    batchLatch.set(new CountDownLatch(1));
                }
            }

            runBatchPostProcessing(filePath);
            batchLatch.get().countDown();
        }
    }

    private void runBatchPostProcessing(Path filePath) {
        logImportInfo("=========== UPDATING THE LOGICAL LINKS...", LogStatus.POST_PROCESSING, "LINKS", 0);
        var logicalLinksResult = ExceptionUtils.tryCatchLog(
                () -> db.callLogicalLinksRefresh(),
                ex -> logImportError(
                        "Error updating the logical links while postprocessing a batch.",
                        ex,
                        filePath.toString()
                )
        );
        if (logicalLinksResult != null) {
            logImportInfo(
                    "=========== Finished updating the logical links. Inserted new links: " + logicalLinksResult,
                    LogStatus.SAVED,
                    "LINKS",
                    0
            );
        }

        logImportInfo("=========== UPDATING THE LEXICON...", LogStatus.POST_PROCESSING, "LEXICON", 0);
        var lexiconResult = ExceptionUtils.tryCatchLog(
                () -> lexiconService.updateLexicon(false),
                ex -> logImportError(
                        "Error updating the lexicon while postprocessing a batch.",
                        ex,
                        filePath.toString()
                )
        );
        if (lexiconResult != null) {
            logImportInfo(
                    "=========== Finished updating the lexicon. Inserted new lex: " + lexiconResult,
                    LogStatus.SAVED,
                    "LEXICON",
                    0
            );
        }

        logImportInfo("=========== UPDATING THE GEONAME LOCATIONS...", LogStatus.POST_PROCESSING, "GEONAME_LOCATION", 0);
        var geonameLocationResult = ExceptionUtils.tryCatchLog(
                () -> db.callGeonameLocationRefresh(),
                ex -> logImportError(
                        "Error updating the geoname locations while postprocessing a batch.",
                        ex,
                        filePath.toString()
                )
        );
        if (geonameLocationResult != null) {
            logImportInfo(
                    "=========== Finished updating the geoname locations. Inserted new locations: " + geonameLocationResult,
                    LogStatus.SAVED,
                    "GEONAME_LOCATION",
                    0
            );
        }

        logImportInfo("=========== POSTPROCESSING THE CORPUS...", LogStatus.POST_PROCESSING, "CORPUS", 0);
        postProccessCorpus(corpus, corpusConfig);
        logImportInfo("=========== FINISHED POSTPROCESSING THE CORPUS...", LogStatus.POST_PROCESSING, "CORPUS", 0);
    }

    /**
     * Tries to store a import log; if fails, gives a warning but otherwise keeps running.
     */
    private void tryStoreUCEImportLog(ImportLog importLog) {
        ExceptionUtils.tryCatchLog(
                () -> db.saveOrUpdateImportLog(importLog),
                (ex) -> logger.warn("Couldn't store a UCEImport log... operation continues.", ex));
    }


    /**
     * Not only logs to the default file logger, but also as a special ImportLog into the database.
     */
    private void logImportInfo(String message, LogStatus status, String file, long duration) {
        var importLog = new ImportLog(this.importerNumber.toString(), message, status, file, this.importId, duration);
        tryStoreUCEImportLog(importLog);
        logger.info(message);
    }

    /**
     * Not only logs to the default file logger, but also as a special ImportLog into the database.
     */
    private void logImportError(String message, Exception ex, String file) {
        var importLog = new ImportLog(this.importerNumber.toString(), ex.getMessage(), LogStatus.ERROR, file, this.importId, 0);
        tryStoreUCEImportLog(importLog);
        logger.error(message, ex);
    }

    /**
     * Here we apply any postprocessing of a document that isn't DUUI and needs the document to be stored once like
     * the rag vector embeddings.
     */
    protected void postProccessDocument(Document document, Corpus corpus, String filePath) {
        logImportInfo("Postprocessing " + filePath, LogStatus.POST_PROCESSING, filePath, 0);
        var start = System.currentTimeMillis();
        var corpusConfig = corpus.getViewModel().getCorpusConfig();

        // Store simple connections between Time, Geonames and Annotation to approximate the question:
        // This annotation occurred in context with this location at this time.
        // TODO: This needs a check if the document already was linked before. Sometimes docs are preprocessed when they already exist.
        if (corpusConfig.getAnnotations().isGeoNames() || corpusConfig.getAnnotations().isTime()) {
            logger.info("Doing contextualized Links between Annotations...");
            // For now we assume that, IF the annotations are on the same page, they are somewhat linked.
            // For now, we link NamedEntities and taxa to Geonames and/or Time
            for (var page : document.getPages()) {

                // Geonames and Times
                var contextAnnotations = new ArrayList<UIMAAnnotation>();
                if (corpusConfig.getAnnotations().isGeoNames())
                    contextAnnotations.addAll(document.getGeoNames().stream().filter(g -> g.getBegin() >= page.getBegin() && g.getEnd() <= page.getEnd()).toList());
                if (corpusConfig.getAnnotations().isTime())
                    contextAnnotations.addAll(document.getTimes().stream().filter(g -> g.getBegin() >= page.getBegin() && g.getEnd() <= page.getEnd()).toList());

                // Link them to other annotations, such as Taxa, NamedEntity (e.g. PERSON)
                var linkedAnnotations = new ArrayList<UIMAAnnotation>();
                if (corpusConfig.getAnnotations().isNamedEntity() && document.getNamedEntities() != null)
                    linkedAnnotations.addAll(document.getNamedEntities().stream().filter(g -> !g.getType().equals("LOCATION") && g.getBegin() >= page.getBegin() && g.getEnd() <= page.getEnd()).toList());
                if (corpusConfig.getAnnotations().getTaxon().isAnnotated() && document.getAllTaxa() != null) {
                    linkedAnnotations.addAll(document.getGazetteerTaxons().stream().filter(g -> g.getBegin() >= page.getBegin() && g.getEnd() <= page.getEnd()).toList());
                    linkedAnnotations.addAll(document.getGnFinderTaxons().stream().filter(g -> g.getBegin() >= page.getBegin() && g.getEnd() <= page.getEnd()).toList());
                }

                // We link FROM the ANNOTATION TO the CONTEXT
                var links = new ArrayList<AnnotationLink>();
                for (var context : contextAnnotations) {
                    for (var anno : linkedAnnotations) {
                        var annoLink = new AnnotationLink();
                        annoLink.setCorpusId(corpus.getId());
                        annoLink.setFrom(document.getDocumentId());
                        annoLink.setTo(document.getDocumentId());
                        annoLink.setType(context instanceof Time ? "time" : "location");
                        annoLink.setLinkId("context");
                        annoLink.setFromId(anno.getId());
                        annoLink.setToId(context.getId());
                        annoLink.setFromAnnotationTypeTable(ReflectionUtils.getTableAnnotationName(anno.getClass()));
                        annoLink.setToAnnotationTypeTable(ReflectionUtils.getTableAnnotationName(context.getClass()));
                        annoLink.setFromAnnotationType(anno.getClass().getName());
                        annoLink.setToAnnotationType(context.getClass().getName());
                        annoLink.setFromCoveredText(anno.getCoveredText());
                        annoLink.setToCoveredText(context.getCoveredText());
                        annoLink.setFromBegin(anno.getBegin());
                        annoLink.setToBegin(context.getBegin());
                        annoLink.setFromEnd(anno.getEnd());
                        annoLink.setToEnd(context.getEnd());

                        links.add(annoLink);
                    }
                }
                ExceptionUtils.tryCatchLog(() -> db.saveOrUpdateManyAnnotationLinks(links),
                        (ex) -> logImportError("Couldn't build contextual annotation links while postprocessing.", ex, filePath));
            }
        }

        // Calculate embeddings if they are activated
        if (corpusConfig.getOther().isEnableEmbeddings()) {
            logger.info("Embeddings...");

            // Sentence Embeddings
            /* TODO: Dont see the sentence embeddings use case yet and they are really ressource heavy.
            var docHasSentenceEmbeddings = ExceptionUtils.tryCatchLog(
                    () -> ragService.documentHasDocumentSentenceEmbeddings(document.getId()),
                    (ex) -> logImportError("Error while checking if a document already has DocumentSentenceEmbeddings.", ex, filePath));
            if (docHasSentenceEmbeddings != null && !docHasSentenceEmbeddings) {
                // Build the sentences, which are the most crucial embeddings
                var documentSentenceEmbeddings = ExceptionUtils.tryCatchLog(
                        () -> ragService.getSentenceEmbeddingFromDocument(document),
                        (ex) -> logImportError("Error getting the complete embedding sentences for document: " + document.getId(), ex, filePath));

                // Store the sentences
                if (documentSentenceEmbeddings != null)
                    for (var docEmbedding : documentSentenceEmbeddings) {
                        ExceptionUtils.tryCatchLog(
                                () -> ragService.saveDocumentSentenceEmbedding(docEmbedding),
                                (ex) -> logImportError("Error saving a document sentence embeddings.", ex, filePath)
                        );
                    }
            }*/

            // Chunk Embeddings
            var docHasChunkEmbeddings = ExceptionUtils.tryCatchLog(
                    () -> embeddingService.documentHasDocumentChunkEmbeddings(document.getId()),
                    (ex) -> logImportError("Error while checking if a document already has DocumentChunkEmbeddings.", ex, filePath));
            if (docHasChunkEmbeddings != null && !docHasChunkEmbeddings) {
                // Build the chunks, which are the most crucial embeddings
                var documentChunkEmbeddings = ExceptionUtils.tryCatchLog(
                        () -> embeddingService.getCompleteEmbeddingChunksFromDocument(document),
                        (ex) -> logImportError("Error getting the complete embedding chunks for document: " + document.getId(), ex, filePath));

                // Store the chunks
                if (documentChunkEmbeddings != null)
                    for (var docEmbedding : documentChunkEmbeddings) {
                        ExceptionUtils.tryCatchLog(
                                () -> embeddingService.saveDocumentChunkEmbedding(docEmbedding),
                                (ex) -> logImportError("Error saving a document chunk embeddings.", ex, filePath)
                        );
                    }
            }

            // Document Embedding
            var docHasEmbedding = ExceptionUtils.tryCatchLog(
                    () -> embeddingService.documentHasDocumentEmbedding(document.getId()),
                    (ex) -> logImportError("Error while checking if a document already has a DocumentEmbedding.", ex, filePath));
            if (docHasEmbedding != null && !docHasEmbedding) {
                // Build a single document embeddings for the whole text
                var documentEmbedding = ExceptionUtils.tryCatchLog(
                        () -> embeddingService.getCompleteEmbeddingFromDocument(document),
                        (ex) -> logImportError("Error getting the complete embedding from a document.", ex, filePath));

                // Store the single document embedding
                if (documentEmbedding != null)
                    ExceptionUtils.tryCatchLog(
                            () -> embeddingService.saveDocumentEmbedding(documentEmbedding),
                            (ex) -> logImportError("Error saving a document embedding.", ex, filePath));
            }
        }

        if (corpusConfig.getOther().isIncludeKeywordDistribution()) {
            logger.info("Keyword Distribution...");

            // Calculate the page keyword distribution if activated
            for (var page : document.getPages()) {
                // If this page already has a keyword dist, continue.
                if (page.getPageKeywordDistribution() != null) continue;

                var KeywordDistribution = ExceptionUtils.tryCatchLog(
                        () -> embeddingService.getTextKeywordDistribution(PageKeywordDistribution.class, page.getCoveredText(document.getFullText())),
                        (ex) -> logImportError("Error getting the PageKeywordDistribution - the postprocessing continues. Document id: " + document.getId(), ex, filePath));
                if (KeywordDistribution == null) continue;

                KeywordDistribution.setBegin(page.getBegin());
                KeywordDistribution.setEnd(page.getEnd());
                KeywordDistribution.setPage(page);
                KeywordDistribution.setPageId(page.getId());
                page.setPageKeywordDistribution(KeywordDistribution);
                // Store it in the db
                ExceptionUtils.tryCatchLog(() -> db.savePageKeywordDistribution(page),
                        (ex) -> logImportError("Error storing the page keyword distribution - the postprocessing continues.", ex, filePath));
            }

            // And the document topic dist if this wasn't added before.
            if (document.getDocumentKeywordDistribution() == null) {
                var documentKeywordDistribution = ExceptionUtils.tryCatchLog(
                        () -> embeddingService.getTextKeywordDistribution(DocumentKeywordDistribution.class, document.getFullText()),
                        (ex) -> logImportError("Error getting the DocumentKeywordDistribution - the postprocessing ends now. Document id: " + document.getId(), ex, filePath));
                if (documentKeywordDistribution == null) return;

                documentKeywordDistribution.setDocument(document);
                documentKeywordDistribution.setDocumentId(document.getId());
                document.setDocumentKeywordDistribution(documentKeywordDistribution);
                // Store it
                ExceptionUtils.tryCatchLog(() -> db.saveDocumentKeywordDistribution(document),
                        (ex) -> logImportError("Error storing the document keyword distribution - the postprocessing ends now.", ex, filePath));
            }
        }

        if (corpusConfig.getAnnotations().isUnifiedTopic()) {

            logger.info("Inserting Sentence and Document Topics...");

            try {
                Path insertSentenceTopicsFilePath = Paths.get(commonConfig.getDatabaseScriptsLocation(), "topic/1_updateSentenceTopics.sql");
                var insertSentenceTopicsScript = Files.readString(insertSentenceTopicsFilePath);

                ExceptionUtils.tryCatchLog(
                        () -> db.executeSqlWithoutReturn(insertSentenceTopicsScript),
                        (ex) -> logImportError("Error executing SQL script to populate sentencetopics table", ex, filePath)
                );

                Path insertDocumentTopicsFilePath = Paths.get(commonConfig.getDatabaseScriptsLocation(), "topic/2_updateDocumentTopics.sql");
                var insertDocumentTopicsScript = Files.readString(insertDocumentTopicsFilePath);

                ExceptionUtils.tryCatchLog(
                        () -> db.executeSqlWithoutReturn(insertDocumentTopicsScript),
                        (ex) -> logImportError("Error executing SQL script to populate documenttopicsraw table", ex, filePath)
                );

                logger.info("Successfully created and populated sentencetopics and documenttopicsraw tables");
            } catch (Exception e) {
                logger.error("Error reading or executing SQL script for topic distribution", e);
            }

            logger.info("Topic Three Topics...");

            if (document.getDocumentTopThreeTopics() == null) {
                var topTopics = ExceptionUtils.tryCatchLog(
                        () -> db.getTopTopicsByDocument(document.getId(), 3),
                        (ex) -> logImportError("Error getting top three topics for document: " + document.getId(), ex, filePath));

                if (topTopics != null && !topTopics.isEmpty()) {
                    var documentTopThreeTopics = new DocumentTopThreeTopics();
                    documentTopThreeTopics.setDocument(document);
                    documentTopThreeTopics.setDocumentId(document.getId());

                    if (topTopics.size() >= 1) {
                        Object[] topic1 = topTopics.get(0);
                        documentTopThreeTopics.setTopicOne((String) topic1[0]);
                        documentTopThreeTopics.setTopicOneScore((Double) topic1[1]);
                    }
                    if (topTopics.size() >= 2) {
                        Object[] topic2 = topTopics.get(1);
                        documentTopThreeTopics.setTopicTwo((String) topic2[0]);
                        documentTopThreeTopics.setTopicTwoScore((Double) topic2[1]);
                    }
                    if (topTopics.size() >= 3) {
                        Object[] topic3 = topTopics.get(2);
                        documentTopThreeTopics.setTopicThree((String) topic3[0]);
                        documentTopThreeTopics.setTopicThreeScore((Double) topic3[1]);
                    }

                    document.setDocumentTopThreeTopics(documentTopThreeTopics);

                    ExceptionUtils.tryCatchLog(
                            () -> db.saveDocumentTopThreeTopics(document),
                            (ex) -> logImportError("Error storing document top three topics", ex, filePath));

                    logImportInfo("Successfully added top three topics to document: " + document.getId(), LogStatus.SAVED, filePath, System.currentTimeMillis() - start);
                }
            }
        }

        logImportInfo("Successfully post processed document " + filePath, LogStatus.SAVED, filePath, System.currentTimeMillis() - start);
        document.setPostProcessed(true);
        ExceptionUtils.tryCatchLog(() -> db.updateDocument(document), (ex) -> logImportError("Couldn't save the document postprocessing flag", ex, filePath));
    }
    /**
     * Apply any postprocessing once the corpus is finished calculating. This will be called even
     * when the corpus import didn't finish due to an error. We still postprocess what we have.
     */
    protected void postProccessCorpus(Corpus corpus, CorpusConfig corpusConfig) {
        logger.info("Postprocessing the Corpus " + corpus.getName());

        // Calculate the tsne reductions of the whole corpus and finally the tsne plot
        if (corpusConfig.getOther().isEnableEmbeddings()) {
            logger.info("Embeddings...");

            // The corpus can be gigantic and we cant pass hundreds of thousand of embeddings into
            // a rest API and perform reductions on them. Instead, we sample them.
            var corpusDocuments = ExceptionUtils.tryCatchLog(() -> db.getNonePostprocessedDocumentsByCorpusId(corpus.getId()),
                    (ex) -> logger.error("Error while fetching none postprocessed documents of corpus with id " + corpus.getId(), ex));
            if (corpusDocuments == null) return;

            Collections.shuffle(corpusDocuments); // We want random samples of size CHUNKSIZE
            var chunked = ListUtils.partitionList(corpusDocuments, 100);

            for (var documents : chunked) {
                // Get the complete list of document sentence embeddings of all documents
                var docSentenceEmbeddings = documents.stream()
                        .flatMap(d -> ExceptionUtils.tryCatchLog(
                                () -> embeddingService.getDocumentSentenceEmbeddingsOfDocument(d.getId()).stream(),
                                (ex) -> logger.error("Error getting the document sentence embeddings of document " + d.getId(), ex)))
                        .filter(Objects::nonNull)
                        .toList();

                // Now, from these sentences - generate a 2D and 3D tsne reduction embedding and store it
                // with the single document embedding
                var reducedSEmbeddingDto = ExceptionUtils.tryCatchLog(
                        () -> embeddingService.getEmbeddingDimensionReductions(
                                docSentenceEmbeddings.stream().map(DocumentSentenceEmbedding::getEmbedding).toList()),
                        (ex) -> logger.error("Error getting embedding dimension reductions in post processing a corpus.", ex));

                if (!(reducedSEmbeddingDto == null || reducedSEmbeddingDto.getTsne2D() == null)) {
                    // Store the tsne reduction in each sentence - this is basically now a 2D and 3D coordinate
                    for (var i = 0; i < reducedSEmbeddingDto.getTsne2D().length; i++) {
                        docSentenceEmbeddings.get(i).setTsne2d(reducedSEmbeddingDto.getTsne2D()[i]);
                        docSentenceEmbeddings.get(i).setTsne3d(reducedSEmbeddingDto.getTsne3D()[i]);
                    }
                    // Update the changes (Could be a bulk Update... let's see :-)
                    docSentenceEmbeddings.forEach(de -> ExceptionUtils.tryCatchLog(
                            () -> embeddingService.updateDocumentSentenceEmbedding(de),
                            (ex) -> logger.error("Error updating and saving a document sentence embedding.", ex)));
                }

                // Get the complete list of document chunk embeddings of all documents
                var docChunkEmbeddings = documents.stream()
                        .flatMap(d -> ExceptionUtils.tryCatchLog(
                                () -> embeddingService.getDocumentChunkEmbeddingsOfDocument(d.getId()).stream(),
                                (ex) -> logger.error("Error getting the document chunk embeddings of document " + d.getId(), ex)))
                        .filter(Objects::nonNull)
                        .toList();

                // Now, from these chunks - generate a 2D and 3D tsne reduction embedding and store it
                // with the single document embedding
                var reducedEmbeddingDto = ExceptionUtils.tryCatchLog(
                        () -> embeddingService.getEmbeddingDimensionReductions(
                                docChunkEmbeddings.stream().map(DocumentChunkEmbedding::getEmbedding).toList()),
                        (ex) -> logger.error("Error getting embedding dimension reductions in post processing a corpus.", ex));

                if (reducedEmbeddingDto == null || reducedEmbeddingDto.getTsne2D() == null) continue;
                // Store the tsne reduction in each chunk - this is basically now a 2D and 3D coordinate
                for (var i = 0; i < reducedEmbeddingDto.getTsne2D().length; i++) {
                    docChunkEmbeddings.get(i).setTsne2D(reducedEmbeddingDto.getTsne2D()[i]);
                    docChunkEmbeddings.get(i).setTsne3D(reducedEmbeddingDto.getTsne3D()[i]);
                }
                // Update the changes (Could be a bulk Update... let's see :-)
                docChunkEmbeddings.forEach(de -> ExceptionUtils.tryCatchLog(
                        () -> embeddingService.updateDocumentChunkEmbedding(de),
                        (ex) -> logger.error("Error updating and saving a document chunk embedding.", ex)));

                // And calculate a reduced embedding for the whole document as well!
                for (var document : documents) {
                    var documentEmbedding = ExceptionUtils.tryCatchLog(
                            () -> embeddingService.getDocumentEmbeddingOfDocument(document.getId()),
                            (ex) -> logger.error("Error getting the document embeddings of document: " + document.getId(), ex));
                    if (documentEmbedding == null) continue;
                    var chunkEmbeddingsOfDocument = docChunkEmbeddings
                            .stream()
                            .filter(e -> e.getDocument_id() == document.getId())
                            .toList();

                    // And mean pool the tsne chunk embeddings for the whole document
                    documentEmbedding.setTsne2d(EmbeddingUtils.meanPooling(chunkEmbeddingsOfDocument
                            .stream()
                            .map(DocumentChunkEmbedding::getTsne2D)
                            .toList()));
                    documentEmbedding.setTsne3d(EmbeddingUtils.meanPooling(chunkEmbeddingsOfDocument
                            .stream()
                            .map(DocumentChunkEmbedding::getTsne3D)
                            .toList()));
                    // Mark it as fully post processed
                    document.setPostProcessed(true);
                    ExceptionUtils.tryCatchLog(() -> db.updateDocument(document),
                            (ex) -> logger.error("Error updating the document while post processing corpus. Postprocessing continues", ex));

                    // Update the document embedding
                    ExceptionUtils.tryCatchLog(
                            () -> embeddingService.updateDocumentEmbedding(documentEmbedding),
                            (ex) -> logger.error("Error updating and saving a document embedding.", ex));
                }
            }

            // Update: we used to calculate a tsne plot here, but we replace this in the future. This didnt work well
            // anyways.
            /*logger.info("Corpus TSNE Plot...");

            // Now that we have the reduced coordinates, lets plot a tsne plot of the corpus and cache it!
            // If we have an existing plot, then update that
            var corpusTsnePlot = corpus.getCorpusTsnePlot();
            if (corpusTsnePlot == null) {
                corpusTsnePlot = new CorpusTsnePlot();
            }
            var htmlPlot = ExceptionUtils.tryCatchLog(
                    () -> ragService.getCorpusTsnePlot(corpus.getId()),
                    (ex) -> logger.error("Error building the corpus tsne plot of corpus: " + corpus.getId(), ex));
            if (htmlPlot == null) return;
            corpusTsnePlot.setPlotHtml(htmlPlot);
            corpusTsnePlot.setCorpus(corpus);
            corpusTsnePlot.setCreated(DateTime.now().toDate());

            // Assign to a final variable because of the weird java restriction of needing effectively
            // final variables for lambda calls. What a shitshow.
            final CorpusTsnePlot finalCorpusTsnePlot = corpusTsnePlot;
            corpus.setCorpusTsnePlot(finalCorpusTsnePlot);

            ExceptionUtils.tryCatchLog(() -> db.saveOrUpdateCorpusTsnePlot(finalCorpusTsnePlot, corpus),
                    (ex) -> logger.error("Error saving or updating the corpus tsne plot.", ex));*/
        }

        if (corpusConfig.getAnnotations().isUnifiedTopic()) {
            logger.info("Inserting into Document and Corpus Topic word tables...");

            try {
                Path insertDocumentTopicWordFilePath = Paths.get(commonConfig.getDatabaseScriptsLocation(), "topic/3_updateDocumentTopicWord.sql");
                var insertDocumentTopicWordScript = Files.readString(insertDocumentTopicWordFilePath);

                ExceptionUtils.tryCatchLog(
                        () -> db.executeSqlWithoutReturn(insertDocumentTopicWordScript),
                        (ex) -> logger.error("Error executing SQL script to populate documenttopicword table", ex)
                );

                Path insertCorpusTopicWordFilePath = Paths.get(commonConfig.getDatabaseScriptsLocation(), "topic/4_updateCorpusTopicWord.sql");
                var insertCorpusTopicWordScript = Files.readString(insertCorpusTopicWordFilePath);

                ExceptionUtils.tryCatchLog(
                        () -> db.executeSqlWithoutReturn(insertCorpusTopicWordScript),
                        (ex) -> logger.error("Error executing SQL script to populate corpustopicword table", ex)
                );

                logger.info("Successfully created and populated word based topic tables");
            } catch (Exception e) {
                logger.error("Error reading or executing SQL script for topic distribution", e);
            }

        }
        logger.info("Done with the corpus postprocessing.");
    }
}