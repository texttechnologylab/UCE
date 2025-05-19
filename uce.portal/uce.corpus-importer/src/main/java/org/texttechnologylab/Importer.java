package org.texttechnologylab;

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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.annotation.DocumentAnnotation;
import org.texttechnologylab.annotation.geonames.GeoNamesEntity;
import org.texttechnologylab.annotation.link.*;
import org.texttechnologylab.annotation.ocr.*;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.config.CorpusConfig;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.biofid.BiofidTaxon;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.corpus.links.AnnotationToDocumentLink;
import org.texttechnologylab.models.corpus.links.DocumentLink;
import org.texttechnologylab.models.corpus.links.DocumentToAnnotationLink;
import org.texttechnologylab.models.corpus.ocr.OCRPageAdapterImpl;
import org.texttechnologylab.models.corpus.ocr.PageAdapter;
import org.texttechnologylab.models.corpus.ocr.PageAdapterImpl;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.imp.ImportLog;
import org.texttechnologylab.models.imp.ImportStatus;
import org.texttechnologylab.models.imp.LogStatus;
import org.texttechnologylab.models.negation.*;
import org.texttechnologylab.models.rag.DocumentChunkEmbedding;
import org.texttechnologylab.models.rag.DocumentSentenceEmbedding;
import org.texttechnologylab.models.topic.TopicValueBase;
import org.texttechnologylab.models.topic.TopicValueBaseWithScore;
import org.texttechnologylab.models.topic.TopicWord;
import org.texttechnologylab.models.topic.UnifiedTopic;
import org.texttechnologylab.services.*;
import org.texttechnologylab.utils.*;
import org.texttechnologylab.models.negation.CompleteNegation;

import java.io.*;
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

public class Importer {

    private static final Gson gson = new Gson();
    private static final Logger logger = LogManager.getLogger(Importer.class);
    private static final int BATCH_SIZE = 10;
    private static final Set<String> WANTED_NE_TYPES = Set.of(
            "LOCATION", "MISC", "PERSON", "ORGANIZATION"
    );
    private static final String[] COMATIBLE_CAS_FILE_ENDINGS = Arrays.asList("xmi", "bz2", "zip", "gz").toArray(new String[0]);
    private static final Set<String> MIME_TYPES_PDF = Set.of("application/pdf", "pdf");
    private GoetheUniversityService goetheUniversityService;
    private PostgresqlDataInterface_Impl db;
    private GbifService gbifService;
    private RAGService ragService;
    private JenaSparqlService jenaSparqlService;
    private String path;
    private String importId;
    private Integer importerNumber;
    private List<UCEMetadataFilter> uceMetadataFilters = new CopyOnWriteArrayList<>(); // need thread safety.
    private LexiconService lexiconService;
    private CommonConfig commonConfig = new CommonConfig();
    private String casView;

    public Importer(ApplicationContext serviceContext,
                    String foldername,
                    int importerNumber,
                    String importId,
                    String casView) {
        initServices(serviceContext);
        this.importerNumber = importerNumber;
        this.importId = importId;
        this.path = foldername;
        this.casView = casView;
    }

    public Importer(ApplicationContext serviceContext) {
        initServices(serviceContext);
    }

    private void initServices(ApplicationContext serviceContext) {
        this.goetheUniversityService = serviceContext.getBean(GoetheUniversityService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.ragService = serviceContext.getBean(RAGService.class);
        this.lexiconService = serviceContext.getBean(LexiconService.class);
        this.jenaSparqlService = serviceContext.getBean(JenaSparqlService.class);
        this.gbifService = serviceContext.getBean(GbifService.class);
    }

    /**
     * Counts the importable UIMA files in the importer path.
     */
    public int getXMICountInPath() {
        if (this.path.isEmpty()) return -1;
        try (var fileStream = Files.walk(Path.of(path))) {
            return (int) fileStream
                    .filter(Files::isRegularFile)
                    .filter(path -> StringUtils.checkIfFileHasExtension(path.toString().toLowerCase(), COMATIBLE_CAS_FILE_ENDINGS))
                    .count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts the importing processing of this instance.
     *
     * @throws DatabaseOperationException
     */
    public void start(int numThreads) throws DatabaseOperationException {
        logger.info(
                "\n _   _ _____  _____   _____                           _   \n" +
                        "| | | /  __ \\|  ___| |_   _|                         | |  \n" +
                        "| | | | /  \\/| |__     | | _ __ ___  _ __   ___  _ __| |_ \n" +
                        "| | | | |    |  __|    | || '_ ` _ \\| '_ \\ / _ \\| '__| __|\n" +
                        "| |_| | \\__/\\| |___   _| || | | | | | |_) | (_) | |  | |_ \n" +
                        " \\___/ \\____/\\____/   \\___/_| |_| |_| .__/ \\___/|_|   \\__|\n" +
                        "                                    | |                   \n" +
                        "                                    |_|"
        );
        logger.info("===========> Global Import Id: " + importId);
        logger.info("===========> Importer Number: " + importerNumber);
        logger.info("===========> Used Threads: " + numThreads);
        logger.info("===========> Importing from path: " + path);
        logger.info("===========> Reading view: " + casView + "\n\n");

        storeCorpusFromFolderAsync(path, numThreads);
    }

    /**
     * Stores an uploaded xmi to a given corpus
     */
    public void storeUploadedXMIToCorpusAsync(InputStream inputStream, Corpus corpus) throws DatabaseOperationException {
        logger.info("Trying to store an uploaded UIMA file...");

        // Before we try to parse the document, we need to check if we have UCEMetadata filters for this corpus.
        if (CorpusConfig.fromJson(corpus.getCorpusJsonConfig()).getAnnotations().isUceMetadata())
            this.uceMetadataFilters = ExceptionUtils.tryCatchLog(
                    () -> new CopyOnWriteArrayList<>(db.getUCEMetadataFiltersByCorpusId(corpus.getId())),
                    (ex) -> logger.error("Couldn't fetch UCEMetadataFilters to a corpus - this shouldn't happen. The process continues without filters.", ex));

        // We don't catch exceptions here, we let them be raised.
        var doc = XMIToDocument(inputStream, corpus, "TODO: CHANGE THIS FILEPATH");
        if (doc == null)
            throw new DatabaseOperationException("The document was already imported into the corpus according to its documentId.");
        db.saveDocument(doc);
        postProccessDocument(doc, corpus, "TODO: CHANGE THIS FILEPATH");

        logger.info("Finished storing and uploaded UIMA file.");
    }

    /**
     * Imports all UIMA xmi files in a folder
     */
    public void storeCorpusFromFolderAsync(String folderName, int numThreads) throws DatabaseOperationException {
        var executor = Executors.newFixedThreadPool(numThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        var corpus = new Corpus();
        CorpusConfig corpusConfig = null;

        if (!SystemStatus.PostgresqlDbStatus.isAlive())
            throw new DatabaseOperationException("Postgresql DB is not alive - cancelling import.");

        // Read the corpus config. If this doesn't exist, we cannot import the corpus
        //fixed paths
        try (var reader = new FileReader(Paths.get(folderName, "corpusConfig.json").toString())) {

            corpusConfig = gson.fromJson(reader, CorpusConfig.class);
            corpus.setName(corpusConfig.getName());
            corpus.setLanguage(corpusConfig.getLanguage());
            corpus.setAuthor(corpusConfig.getAuthor());
            corpus.setCorpusJsonConfig(gson.toJson(corpusConfig));

            // Let's check if we already have a corpus with that name and
            // if we want to add to that in the config.
            if (corpusConfig.isAddToExistingCorpus()) {
                final var corpusConfig1 = corpusConfig; // This sucks so hard - why doesn't java just do this itself if needed?
                var existingCorpus = ExceptionUtils.tryCatchLog(() -> db.getCorpusByName(corpusConfig1.getName()),
                        (ex) -> logger.error("Error getting an existing corpus by name. The corpus config should probably be changed " +
                                "to not add to existing corpus then.", ex));

                if (existingCorpus != null) { // If we have the corpus, use that. Else store the new corpus.
                    corpus = existingCorpus;
                    // In case that we have a corpus already, we load the existing filters if they exist.
                    if (corpusConfig.getAnnotations().isUceMetadata())
                        this.uceMetadataFilters = new CopyOnWriteArrayList<>(db.getUCEMetadataFiltersByCorpusId(corpus.getId()));
                } else {
                    final var corpus1 = corpus;
                    ExceptionUtils.tryCatchLog(() -> db.saveCorpus(corpus1),
                            (ex) -> logger.error("Error saving the corpus.", ex));
                }
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            throw new MissingResourceException(
                    "The corpus folder did not contain a properly formatted corpusConfig.json", CorpusConfig.class.toString(), "");
        }

        // Store some corpus information in the UCEImport logging if this is the main importer
        if (this.importerNumber == 1) {
            var uceImport = db.getUceImportByImportId(this.importId);
            uceImport.setTargetCorpusName(corpus.getName());
            uceImport.setTargetCorpusId(corpus.getId());
            uceImport.setStatus(ImportStatus.RUNNING);
            db.saveOrUpdateUceImport(uceImport);
        }

        var inputFolderName = Path.of(folderName, "input");
        final var corpusConfigFinal = corpusConfig;
        final var corpus1 = corpus;

        // Needed for parallel processing
        var docInBatch = new AtomicInteger(0);
        var lock = new Object();
        var batchLatch = new AtomicReference<>(new CountDownLatch(0));

        try (var fileStream = Files.walk(inputFolderName)) {
            fileStream.filter(Files::isRegularFile)
                    .filter(path -> StringUtils.checkIfFileHasExtension(path.toString().toLowerCase(), COMATIBLE_CAS_FILE_ENDINGS))
                    .forEach(filePath -> {
                        var docFuture = CompletableFuture.supplyAsync(() -> {
                                    try {
                                        batchLatch.get().await(); // wait if a batch is being postprocessed
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }

                                    return XMIToDocument(filePath.toString(), corpus1);
                                }, executor)
                                .thenApply(doc -> {
                                    if (doc == null) return null;

                                    logger.info("Trying to store document with document id " + doc.getDocumentId() + "...");
                                    ExceptionUtils.tryCatchLog(
                                            () -> db.saveDocument(doc),
                                            (ex) -> logImportError("Error saving document with id " + doc.getId(), ex, filePath.toString()));
                                    return doc;
                                })
                                .thenAcceptAsync(doc -> {
                                    if (doc != null) {
                                        logImportInfo("Stored document " + filePath.getFileName(), LogStatus.SAVED, filePath.toString(), 0);
                                        logger.info("Finished with the UIMA annotations - postprocessing the doc now.");

                                        ExceptionUtils.tryCatchLog(
                                                () -> postProccessDocument(doc, corpus1, filePath.toString()),
                                                (ex) -> logImportError("Error postprocessing a saved document with id " + doc.getId(), ex, filePath.toString()));
                                        logImportInfo("Finished with import.", LogStatus.FINISHED, filePath.toString(), 0);
                                    }

                                    int local = docInBatch.incrementAndGet();
                                    if (local == BATCH_SIZE) {
                                        synchronized (lock) {
                                            // Double-check to avoid race
                                            if (docInBatch.get() == BATCH_SIZE) {
                                                docInBatch.set(0);
                                                batchLatch.set(new CountDownLatch(1));
                                            }
                                        }

                                        // Block other threads by not releasing the latch yet. We want the postprocessing being done by a single thread,
                                        // while all the others wait.
                                        logImportInfo("=========== UPDATING THE LOGICAL LINKS...", LogStatus.POST_PROCESSING, "LINKS", 0);
                                        var logicalLinksResult = ExceptionUtils.tryCatchLog(
                                                () -> db.callLogicalLinksRefresh(),
                                                (ex) -> logImportError("Error updating the logical links while postprocessing a batch.", ex, filePath.toString()));
                                        if (logicalLinksResult != null)
                                            logImportInfo("=========== Finished updating the logical links. Inserted new links: " + logicalLinksResult, LogStatus.SAVED, "LINKS", 0);

                                        logImportInfo("=========== UPDATING THE LEXICON...", LogStatus.POST_PROCESSING, "LEXICON", 0);
                                        var lexiconResult = ExceptionUtils.tryCatchLog(
                                                () -> lexiconService.updateLexicon(false),
                                                (ex) -> logImportError("Error updating the lexicon while postprocessing a batch.", ex, filePath.toString()));
                                        if (lexiconResult != null)
                                            logImportInfo("=========== Finished updating the lexicon. Inserted new lex: " + lexiconResult, LogStatus.SAVED, "LEXICON", 0);

                                        logImportInfo("=========== UPDATING THE GEONAME LOCATIONS...", LogStatus.POST_PROCESSING, "GEONAME_LOCATION", 0);
                                        var geonameLocationResult = ExceptionUtils.tryCatchLog(
                                                () -> db.callGeonameLocationRefresh(),
                                                (ex) -> logImportError("Error updating the geoname locations while postprocessing a batch.", ex, filePath.toString()));
                                        if (geonameLocationResult != null)
                                            logImportInfo("=========== Finished updating the geoname locations. Inserted new locations: " + geonameLocationResult, LogStatus.SAVED, "GEONAME_LOCATION", 0);

                                        logImportInfo("=========== POSTPROCESSING THE CORPUS...", LogStatus.POST_PROCESSING, "CORPUS", 0);
                                        postProccessCorpus(corpus1, corpusConfigFinal);
                                        logImportInfo("=========== FINISHED POSTPROCESSING THE CORPUS...", LogStatus.POST_PROCESSING, "CORPUS", 0);

                                        // Allow all others to continue
                                        batchLatch.get().countDown();
                                    }
                                });

                        futures.add(docFuture);
                    });

        } catch (IOException ex) {
            logger.error("Error walking the import path: " + inputFolderName, ex);
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Final links updating
        ExceptionUtils.tryCatchLog(
                () -> db.callLogicalLinksRefresh(),
                (ex) -> logger.error("Error in the final logical links update of the current corpus with id " + corpus1.getId()));

        // Final lexicon updating
        ExceptionUtils.tryCatchLog(
                () -> lexiconService.updateLexicon(false),
                (ex) -> logger.error("Error in the final lexicon update of the current corpus with id " + corpus1.getId()));

        // Final geonames location updating
        ExceptionUtils.tryCatchLog(
                () -> db.callGeonameLocationRefresh(),
                (ex) -> logger.error("Error in the final geoname location update of the current corpus with id " + corpus1.getId()));

        // Final corpus postprocessing
        ExceptionUtils.tryCatchLog(
                () -> postProccessCorpus(corpus1, corpusConfigFinal),
                (ex) -> logger.error("Error in the final postprocessing of the current corpus with id " + corpus1.getId()));

        logger.info("\n\n=================================\n Done with the corpus import.");
        executor.shutdown();
    }

    /**
     * Converts an XMI inputstream to a Document.
     */
    public Document XMIToDocument(InputStream inputStream, Corpus corpus, String filePath) {
        try {
            var jCas = JCasFactory.createJCas();
            CasIOUtils.load(inputStream, null, jCas.getCas(), CasLoadMode.LENIENT);

            return XMIToDocument(jCas, corpus, filePath);
        } catch (Exception ex) {
            logger.error("Error while reading an annotated xmi file from stream to a cas and transforming it into a document:", ex);
            return null;
        }
    }

    /**
     * Converts an XMI to a Document by path
     */
    public Document XMIToDocument(String filename, Corpus corpus) {
        try {
            var jCas = JCasFactory.createJCas();
            try (InputStream inputStream = openInputStreamBasedOnExtension(filename)) {
                if (inputStream == null) {
                    logImportError("Unsupported file type or failed to open stream: " + filename, new NullPointerException("Stream was null"), filename);
                    return null;
                }
                // https://uima.apache.org/d/uimaj-current/api/org/apache/uima/util/CasIOUtils.html
                // tsiInputStream: Optional stream for typesystem - only used if not null. (which it currently is)
                CasIOUtils.load(inputStream, null, jCas.getCas(), CasLoadMode.LENIENT);

                // Import from a specific view, if given
                if (casView != null) {
                    jCas = jCas.getView(casView);
                }
            }

            return XMIToDocument(jCas, corpus, filename);
        } catch (Exception ex) {
            logger.error("Error while reading an annotated xmi file to a cas and transforming it into a document:", ex);
            return null;
        }
    }

    /**
     * Loads and returns an Input stream for a cas depending on the compression
     */
    private InputStream openInputStreamBasedOnExtension(String filename) throws IOException {
        var file = new File(filename);
        var lowerName = filename.toLowerCase();

        if (lowerName.endsWith(".xmi")) {
            return new FileInputStream(file);

        } else if (lowerName.endsWith(".gz")) {
            return new GZIPInputStream(new FileInputStream(file));

        } else if (lowerName.endsWith(".bz2")) {
            return new BZip2CompressorInputStream(new FileInputStream(file));

        } else if (lowerName.endsWith(".zip")) {
            var zipStream = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry;

            while ((entry = zipStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xmi")) {
                    // ZipInputStream itself acts as the stream for the contained file
                    return zipStream;
                }
            }

            zipStream.close();
            return null;
        }

        return null; // Unsupported
    }

    /**
     * Convert a UIMA jCas to an OCRDocument
     */
    public Document XMIToDocument(JCas jCas, Corpus corpus, String filePath) {

        logger.info("=============================== Importing a new CAS as a Document. ===============================");

        // Read in the contents of a single xmi cas to see what's inside
        var unique = new HashSet<String>();
        JCasUtil.select(jCas, AnnotationBase.class).forEach(a -> {
            unique.add(a.getType().getName());
        });
        //unique.forEach(logger::info);

        try {
            // Corpus config so we know what do look for
            var corpusConfig = gson.fromJson(corpus.getCorpusJsonConfig(), CorpusConfig.class);

            // First, metadata
            var metadata = JCasUtil.selectSingle(jCas, DocumentMetaData.class);
            if (metadata == null) {
                // If the metadata block is missing, something is off. In that case, return null
                return null;
            }
            var document = new Document(metadata.getLanguage(),
                    metadata.getDocumentTitle(),
                    metadata.getDocumentId(),
                    corpus.getId());
            logger.info("Setting Metadata done.");
            logImportInfo("Importing " + document.getDocumentId(), LogStatus.CAS_IMPORT, filePath, 0);
            // We track how long each document import takes.
            var start = System.currentTimeMillis();

            // Before we parse and add that document, lets check if a document with that id and in that
            // corpus already exists. If we created a new corpus, this will always be null.
            var exists = db.documentExists(corpus.getId(), document.getDocumentId());
            if (exists) {
                logger.info("Document with id " + document.getDocumentId()
                        + " already exists in the corpus " + corpus.getId() + ".");
                logger.info("Checking if that document was also post-processed yet...");
                var existingDoc = db.getDocumentByCorpusAndDocumentId(corpus.getId(), document.getDocumentId());
                if (!existingDoc.isPostProcessed()) {
                    logger.info("Not yet post-processed. Doing that now.");
                    postProccessDocument(existingDoc, corpus, filePath);
                }
                logger.info("Done.");
                return null;
            }

            // set the mime type
            document.setMimeType(jCas.getSofaMimeType());

            // Set the full text
            if (MIME_TYPES_PDF.contains(document.getMimeType())) {
                document.setFullText("");

                // PDF is just bytes in the SofA
                // TODO support different ways of storing the PDF?
                byte[] pdfBytes = jCas.getSofaDataStream().readAllBytes();
                document.setDocumentData(pdfBytes);
                logger.info("Document is a PDF: " + document.getMimeType() + " of length " + pdfBytes.length);
            } else {
                // by default, we assume text as before
                document.setFullText(jCas.getDocumentText());
                logger.info("Setting full text done.");
            }

            setMetadataTitleInfo(document, jCas, corpusConfig);

            // For now, we skip this. This doesn't relly improve anything and is very costly.
            //setCleanedFullText(document, jCas);
            if (corpusConfig.getAnnotations().isUceMetadata())
                ExceptionUtils.tryCatchLog(
                        () -> setUceMetadata(document, jCas, corpus.getId()),
                        (ex) -> logImportWarn("This file should have contained UceMetadata annotations, but selecting them caused an error.", ex, filePath));

            if (corpusConfig.getAnnotations().isSentence())
                ExceptionUtils.tryCatchLog(
                        () -> setSentences(document, jCas),
                        (ex) -> logImportWarn("This file should have contained sentence annotations, but selecting them caused an error.", ex, filePath));

            if (corpusConfig.getAnnotations().isNamedEntity())
                ExceptionUtils.tryCatchLog(
                        () -> setNamedEntities(document, jCas),
                        (ex) -> logImportWarn("This file should have contained ner annotations, but selecting them caused an error.", ex, filePath));

            // GeoNames requires both GeoName and NamedEntity annotations.
            if (corpusConfig.getAnnotations().isNamedEntity() && corpusConfig.getAnnotations().isGeoNames())
                ExceptionUtils.tryCatchLog(
                        () -> setGeoNames(document, jCas),
                        (ex) -> logImportWarn("This file should have contained GeoNames annotations, but selecting them caused an error.", ex, filePath));

            if (corpusConfig.getAnnotations().isLemma())
                ExceptionUtils.tryCatchLog(
                        () -> setLemmata(document, jCas),
                        (ex) -> logImportWarn("This file should have contained lemmata annotations, but selecting them caused an error.", ex, filePath));

            if (corpusConfig.getAnnotations().isSrLink())
                ExceptionUtils.tryCatchLog(
                        () -> setSemanticRoleLabels(document, jCas),
                        (ex) -> logImportWarn("This file should have contained SRL annotations, but selecting them caused an error.", ex, filePath));

            if (corpusConfig.getAnnotations().isTime())
                ExceptionUtils.tryCatchLog(
                        () -> setTimes(document, jCas),
                        (ex) -> logImportWarn("This file should have contained time annotations, but selecting them caused an error.", ex, filePath));

            if (corpusConfig.getAnnotations().getTaxon().isAnnotated())
                ExceptionUtils.tryCatchLog(
                        () -> setTaxonomy(document, jCas, corpusConfig),
                        (ex) -> logImportWarn("This file should have contained taxon annotations, but selecting them caused an error.", ex, filePath));

            if (corpusConfig.getAnnotations().isWikipediaLink())
                ExceptionUtils.tryCatchLog(
                        () -> setWikiLinks(document, jCas),
                        (ex) -> logImportWarn("This file should have contained wiki links annotations, but selecting them caused an error.", ex, filePath));

            if (corpusConfig.getAnnotations().isCompleteNegation())
                ExceptionUtils.tryCatchLog(
                        () -> setCompleteNegations(document, jCas),
                        (ex) -> logImportWarn("This file should have contained negation annotations, but selecting them caused an error.", ex, filePath));

            if (corpusConfig.getAnnotations().isUnifiedTopic())
                ExceptionUtils.tryCatchLog(
                        () -> setUnifiedTopic(document, jCas),
                        (ex) -> logImportWarn("This file should have contained UnifiedTopic annotations, but selecting them caused an error.", ex, filePath));

            // Keep this at the end of the annotation setting, as they might require previous annotations. Order matter here!
            if (corpusConfig.getAnnotations().isLogicalLinks())
                ExceptionUtils.tryCatchLog(
                        () -> setLogicLinks(document, jCas, corpus.getId(), filePath),
                        (ex) -> logImportWarn("This file should have contained LinkTypesystem annotations, but selecting them caused an error.", ex, filePath));

            ExceptionUtils.tryCatchLog(
                    () -> setPages(document, jCas, corpusConfig),
                    (ex) -> logImportWarn("This file should have contained OCRPage annotations, but selecting them caused an error.", ex, filePath));

            var duration = System.currentTimeMillis() - start;
            logImportInfo("Successfully extracted all annotations from " + filePath, LogStatus.FINISHED, filePath, duration);

            return document;
        } catch (Exception ex) {
            logImportError("Unknown error while importing a CAS into a document. This shouldn't happen, as each operation has its own error handling.", ex, filePath);
            return null;
        } finally {
            logger.info("Finished with importing that CAS.\n\n\n");
        }
    }

    /**
     * Selects and set the geoNames of a document.
     */
    private void setGeoNames(Document document, JCas jCas) {
        var geoNames = new ArrayList<GeoName>();
        JCasUtil.select(jCas, GeoNamesEntity.class).forEach(g -> {
            var geoName = new GeoName(g.getBegin(), g.getEnd());
            geoName.setCoveredText(g.getCoveredText());
            geoName.setName(g.getName());
            geoName.setFeatureClass(GeoNameFeatureClass.valueOf(g.getFeatureClass()));
            geoName.setFeatureCode(g.getFeatureCode());
            geoName.setCountryCode(g.getCountryCode());
            geoName.setAdm1(g.getAdm1());
            geoName.setAdm2(g.getAdm2());
            geoName.setAdm3(g.getAdm3());
            geoName.setAdm4(g.getAdm4());
            geoName.setLatitude(g.getLatitude());
            geoName.setLongitude(g.getLongitude());
            geoName.setElevation(g.getElevation());
            var referenceNE = g.getReferenceAnnotation();
            if (referenceNE != null) {
                // The NE should have already been extracted and added to the document
                var ne = document.getNamedEntities().stream().filter(
                        n -> n.getBegin() == referenceNE.getBegin() && n.getEnd() == referenceNE.getEnd() && n.getType().equals("LOCATION")).findFirst();
                if (ne.isPresent()) {
                    geoName.setRefNamedEntity(ne.get());
                    ne.get().setGeoName(geoName);
                }
            }
            geoNames.add(geoName);
        });
        document.setGeoNames(geoNames);
        logger.info("Setting GeoNames done.");
    }

    /**
     * Selects and sets the logical links between documents, annotations and more.
     */
    private void setLogicLinks(Document document, JCas jCas, long corpusId, String filePath) throws DatabaseOperationException {
        // Document -> Document Links
        var documentLinks = new ArrayList<DocumentLink>();
        JCasUtil.select(jCas, DLink.class).forEach(l -> {
            var docLink = new DocumentLink();
            docLink.setFrom(l.getFrom());
            docLink.setTo(l.getTo());
            docLink.setLinkId(String.valueOf(l.getLinkId()));
            docLink.setType(l.getLinkType());
            docLink.setCorpusId(corpusId);

            documentLinks.add(docLink);
        });
        // We store the document links not to the Document class directly, as that doesn't fit the idea of the datastructure.
        // Linking should happen *on top* of documents and clusters, not as a part of them. Keep that in mind.
        db.saveOrUpdateManyDocumentLinks(documentLinks);

        // Document -> Annotation Link
        var documentToAnnotationLinks = new ArrayList<DocumentToAnnotationLink>();
        JCasUtil.select(jCas, DALink.class).forEach(l -> {
            var docToAnnoLink = new DocumentToAnnotationLink();
            docToAnnoLink.setCorpusId(corpusId);
            docToAnnoLink.setFrom(l.getFrom()); // from is a documentId, but not of *this* document.
            docToAnnoLink.setLinkId(String.valueOf(l.getLinkId()));
            docToAnnoLink.setType(l.getLinkType());
            // In the case of a Document -> Annotation, the "to" in the typesystem points to the current document
            docToAnnoLink.setTo(document.getDocumentId());

            var toAnnotation = l.getTo();
            docToAnnoLink.setToBegin(toAnnotation.getBegin());
            docToAnnoLink.setToEnd(toAnnotation.getEnd());
            docToAnnoLink.setToCoveredText(toAnnotation.getCoveredText());
            // The toAnnotation points to any kind of annotation *within* the cas. We have to translate that information
            // to UCE's model classes and we do that through Reflection Utils... Reflection is very costly ressourcewise,
            // so we cached a lot upon start, but this may still slow down the process - we have to investigate by how much.
            Class<?> modelClass = ReflectionUtils.findModelClassForCASAnnotation(toAnnotation);
            if (modelClass == null) {
                logImportWarn("A logical Link annotation tried to point to an annotation that UCE doesn't support yet, hence skipped the link.",
                        new InvalidClassException(toAnnotation.getType().getName() + " annotation not supported by UCE."), filePath);
                return;
            }
            docToAnnoLink.setToAnnotationType(modelClass.getName());
            var tableName = ReflectionUtils.getTableAnnotationName(modelClass);
            docToAnnoLink.setToAnnotationTypeTable(tableName);

            documentToAnnotationLinks.add(docToAnnoLink);
        });
        db.saveOrUpdateManyDocumentToAnnotationLinks(documentToAnnotationLinks);

        // Annotation -> Document Link
        var annotationToDocumentLinks = new ArrayList<AnnotationToDocumentLink>();
        JCasUtil.select(jCas, ADLink.class).forEach(l -> {
            var annoToDocLink = new AnnotationToDocumentLink();
            annoToDocLink.setCorpusId(corpusId);
            annoToDocLink.setTo(l.getTo()); // to is a documentId, but not of *this* document.
            annoToDocLink.setLinkId(String.valueOf(l.getLinkId()));
            annoToDocLink.setType(l.getLinkType());
            // In the case of a Annotation -> Document, the "from" in the typesystem points to the current document
            annoToDocLink.setFrom(document.getDocumentId());

            var fromAnnotation = l.getFrom();
            annoToDocLink.setFromBegin(fromAnnotation.getBegin());
            annoToDocLink.setFromEnd(fromAnnotation.getEnd());
            annoToDocLink.setFromCoveredText(fromAnnotation.getCoveredText());
            // Same procedure as in DocumentToAnnotation above; read that comment.
            Class<?> modelClass = ReflectionUtils.findModelClassForCASAnnotation(fromAnnotation);
            if (modelClass == null) {
                logImportWarn("A logical Link annotation tried to point to an annotation that UCE doesn't support yet, hence skipped the link.",
                        new InvalidClassException(fromAnnotation.getType().getName() + " annotation not supported by UCE."), filePath);
                return;
            }
            annoToDocLink.setFromAnnotationType(modelClass.getName());
            var tableName = ReflectionUtils.getTableAnnotationName(modelClass);
            annoToDocLink.setFromAnnotationTypeTable(tableName);

            annotationToDocumentLinks.add(annoToDocLink);
        });
        db.saveOrUpdateManyAnnotationToDocumentLinks(annotationToDocumentLinks);
    }

    /**
     * Selects and sets the times to the document.
     */
    private void setUceMetadata(Document document, JCas jCas, long corpusId) {
        var data = new ArrayList<UCEMetadata>();
        JCasUtil.select(jCas, org.texttechnologylab.annotation.uce.Metadata.class).forEach(t -> {
            var metadata = new UCEMetadata();
            metadata.setComment(t.getComment());
            metadata.setValue(t.getValue());
            metadata.setKey(t.getKey());
            metadata.setValueType(UCEMetadataValueType.valueOf(t.getValueType().toUpperCase()));
            data.add(metadata);

            // Now check if we have added a new distinct metadata. If not, then cache it for filtering later.
            // But we are not interested in filtering for JSON content. That's not feasible.
            if (metadata.getValueType() == UCEMetadataValueType.JSON) return;
            var possibleFilter = this.uceMetadataFilters
                    .stream()
                    .filter(f -> f.getKey().equals(t.getKey()) && metadata.getValueType() == metadata.getValueType())
                    .findFirst();
            // If it's empty, we don't have that metadata cached as a filter yet. So do it.
            if (possibleFilter.isEmpty()) {
                var newFilter = new UCEMetadataFilter(corpusId, metadata.getKey(), metadata.getValueType());
                if (metadata.getValueType() == UCEMetadataValueType.NUMBER) {
                    // We assume, that a number can actually be parsed to a number
                    // TODO should this be configurable? What about integers?
                    var number = StringUtils.tryParseFloat(metadata.getValue());
                    // do not setup a filter if we cant parse the number
                    if (!Float.isNaN(number)) {
                        newFilter.setMin(number);
                        newFilter.setMax(number);
                    }
                } else {
                    newFilter.addPossibleCategory(metadata.getValue());
                }
                synchronized (newFilter) {
                    this.uceMetadataFilters.add(newFilter);
                }
                ExceptionUtils.tryCatchLog(
                        () -> db.saveUCEMetadataFilter(newFilter),
                        (ex) -> logger.error("Tried saving a new UCEMetadataFilter, but got an error: ", ex));
            } else {
                // If this filter already exists, then we need to check if it's an Enum filter. If yes, there is a chance
                // that a new category to that enum must be added and stored. Let's check.
                var existingFilter = possibleFilter.get();
                if (existingFilter.getValueType() == UCEMetadataValueType.ENUM) {
                    // Again, thread safety. If this worker updates a filter category, we need the other to know.
                    synchronized (existingFilter) {
                        if (existingFilter.getPossibleCategories().stream().noneMatch(c -> c.equals(metadata.getValue()))) {
                            existingFilter.addPossibleCategory(metadata.getValue());
                            ExceptionUtils.tryCatchLog(
                                    () -> db.saveOrUpdateUCEMetadataFilter(existingFilter),
                                    (ex) -> logger.error("Tried updating an existing UCEMetadataFilter, but got an error: ", ex));
                        }
                    }
                } else if (existingFilter.getValueType() == UCEMetadataValueType.NUMBER) {
                    // We assume, that a number can actually be parsed to a number
                    // TODO should this be configurable? What about integers?
                    var number = StringUtils.tryParseFloat(metadata.getValue());
                    if (!Float.isNaN(number)) {
                        synchronized (existingFilter) {
                            if (existingFilter.getMin() == null) existingFilter.setMin(number);
                            else if (number < existingFilter.getMin()) {
                                existingFilter.setMin(number);
                            }
                            if (existingFilter.getMax() == null) existingFilter.setMax(number);
                            else if (number > existingFilter.getMax()) {
                                existingFilter.setMax(number);
                            }
                            ExceptionUtils.tryCatchLog(
                                    () -> db.saveOrUpdateUCEMetadataFilter(existingFilter),
                                    (ex) -> logger.error("Tried updating an existing UCEMetadataFilter for type NUMBER, but got an error: ", ex));
                        }
                    }
                }
            }
        });
        document.setUceMetadata(data);
        logger.info("Setting UCE Metadata done.");
    }

    /**
     * Select and set possible metadata. Also adds Goethe Scraping if applicable
     */
    private void setMetadataTitleInfo(Document document, JCas jCas, CorpusConfig corpusConfig) {
        // See if we can get any more information from the goethe collections
        var metadataTitleInfo = new MetadataTitleInfo();
        if (corpusConfig.getOther().isAvailableOnFrankfurtUniversityCollection()) {
            metadataTitleInfo = ExceptionUtils.tryCatchLog(
                    () -> goetheUniversityService.scrapeDocumentTitleInfo(document.getDocumentId()),
                    (ex) -> logger.error("Error scraping the metadata info of the document with id: " + document.getDocumentId(), ex));
            if (metadataTitleInfo != null) document.setMetadataTitleInfo(metadataTitleInfo);
            logger.info("Setting potential metadata title info done.");
        } else {
            // There are possibly additional metadata hidden in the DocumentAnnotation type.
            var documentAnnotation = ExceptionUtils.tryCatchLog(
                    () -> JCasUtil.selectSingle(jCas, DocumentAnnotation.class),
                    (ex) -> logger.info("No DocumentAnnotation found. Skipping this annotation then."));
            if (documentAnnotation != null) {
                try {
                    metadataTitleInfo.setPublished(documentAnnotation.getDateDay() + "."
                            + documentAnnotation.getDateMonth() + "."
                            + documentAnnotation.getDateYear());
                    metadataTitleInfo.setAuthor(documentAnnotation.getAuthor());
                } catch (Exception ex) {
                    logger.warn("Tried extracting DocumentAnnotation type, it caused an error. Import will be continued as usual.");
                }
            }
        }
        document.setMetadataTitleInfo(metadataTitleInfo);
    }

    /**
     * Selects and sets pages to a document.
     */
    private void setPages(Document document, JCas jCas, CorpusConfig corpusConfig) {
        // Set the OCRPages
        if (corpusConfig.getAnnotations().isOCRPage()) {
            var pages = new ArrayList<Page>();

            // We have multiple typesystems for "OCR extracted pages", which have pretty much the same properties
            // To handle that, we use an adapter class and iterate through them the same way. Can't use abstractions with typesystems sadly.
            var pageAdapters = new ArrayList<PageAdapter>();
            JCasUtil.select(jCas, OCRPage.class)
                    .forEach(p -> pageAdapters.add(new OCRPageAdapterImpl(p)));
            // The ABBYY Pages have no pageNumber anymore, so we count up by hand...
            var pageCount = new AtomicInteger(1);
            JCasUtil.select(jCas, org.texttechnologylab.annotation.ocr.abbyy.Page.class)
                    .forEach(p -> {
                        pageAdapters.add(new PageAdapterImpl(p, pageCount.get()));
                        pageCount.getAndIncrement();
                    });

            // We go through each page
            for (var p : pageAdapters) {
                // New page
                var page = new Page(p.getBegin(), p.getEnd(), p.getPageNumber(), p.getPageId());
                page.setDocument(document);
                page.setCoveredText(p.getCoveredText());

                // TODO: For now, only the new ABBYY typesystems get their paragraphs. We dont really use the others anymore.
                // In the future, we can add back blocks and lines and whatnot for a more truer OCR page visualization
                if (corpusConfig.getAnnotations().isOCRParagraph() && p.getOriginal() instanceof org.texttechnologylab.annotation.ocr.abbyy.Page)
                    page.setParagraphs(getCoveredParagraphs((org.texttechnologylab.annotation.ocr.abbyy.Page) p.getOriginal()));

                //if (corpusConfig.getAnnotations().isOCRBlock() && p.getOriginal() instanceof org.texttechnologylab.annotation.ocr.abbyy.Page)
                //    page.setBlocks(getCoveredBlocks((org.texttechnologylab.annotation.ocr.abbyy.Page) p.getOriginal()));

                //if (corpusConfig.getAnnotations().isOCRLine() && p.getOriginal() instanceof org.texttechnologylab.annotation.ocr.abbyy.Page)
                //    page.setLines(getCoveredLines((org.texttechnologylab.annotation.ocr.abbyy.Page) p.getOriginal()));

                updateAnnotationsWithPageId(document, page, false);
                pages.add(page);
            }

            document.setPages(pages);
            logger.info("Setting OCRPages done.");
        } else {
            // If the corpus isn't OCRPage annotated, we create our own pseudo pages.
            // We want pages as our pagination of the document reader relies on it to handle larger documents.
            // In this case: we chunk the whole text into pages
            var fullText = document.getFullText();
            var pageSize = 7500;
            var pageNumber = 1;
            var pages = new ArrayList<Page>();

            for (var i = 0; i < fullText.length(); i += pageSize) {
                var pageEnd = Math.min(i + pageSize, fullText.length());
                var page = new Page(i, pageEnd, pageNumber, "");
                page.setCoveredText(fullText.substring(i, pageEnd));
                page.setDocument(document);
                pageNumber += 1;
                updateAnnotationsWithPageId(document, page, false);

                pages.add(page);
            }
            document.setPages(pages);
            logger.info("Setting synthetic pages done.");
        }

        // Since we have some errors in the annotation (mainly we have an offset in begin and end sometimes),
        // we need to cleanup at the end so that every annotation that doesn't have a page (because of the error offset)
        // is assigned to the last page.
        updateAnnotationsWithPageId(document, document.getPages().getLast(), true);
    }

    private void updateAnnotationsWithPageId(Document document, Page page, boolean isLastPage) {
        // Set the pages for the different annotations
        if (document.getBiofidTaxons() != null) {
            for (var anno : document.getBiofidTaxons().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
        if (document.getTaxons() != null) {
            for (var anno : document.getTaxons().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
        if (document.getNamedEntities() != null) {
            for (var anno : document.getNamedEntities().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
        if (document.getGeoNames() != null) {
            for (var anno : document.getGeoNames().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
        if (document.getTimes() != null) {
            for (var anno : document.getTimes().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
        // negations
        if (document.getCues() != null) {
            for (var anno : document.getCues().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
        if (document.getEvents() != null) {
            for (var anno : document.getEvents().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
        if (document.getFocuses() != null) {
            for (var anno : document.getFocuses().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
        if (document.getScopes() != null) {
            for (var anno : document.getScopes().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
        if (document.getXscopes() != null) {
            for (var anno : document.getXscopes().stream().filter(t ->
                    (t.getBegin() >= page.getBegin() && t.getEnd() <= page.getEnd()) || (t.getPage() == null && isLastPage)).toList()) {
                anno.setPage(page);
            }
        }
    }

    /**
     * Selects and sets the WikiLinks to the document.
     */
    private void setWikiLinks(Document document, JCas jCas) {
        var wikiDatas = new ArrayList<org.texttechnologylab.models.corpus.WikipediaLink>();
        JCasUtil.select(jCas, org.hucompute.textimager.uima.type.wikipedia.WikipediaLink.class).forEach(w -> {
            var data = new org.texttechnologylab.models.corpus.WikipediaLink(w.getBegin(), w.getEnd());
            data.setLinkType(w.getLinkType());
            data.setTarget(w.getTarget());
            data.setCoveredText(w.getCoveredText());
            data.setWikiData(w.getWikiData());
            data.setWikiDataHyponyms(
                    Arrays.stream(w.getWikiDataHyponyms().toArray()).filter(wd -> !wd.isEmpty()).map(WikiDataHyponym::new).toList()
            );

            wikiDatas.add(data);
        });
        document.setWikipediaLinks(wikiDatas);
        logger.info("Setting Wikipedia Links done.");
    }

    /**
     * Selects taxnomies and tries to enrich specific biofid onthologies as well.
     */
    private void setTaxonomy(Document document, JCas jCas, CorpusConfig corpusConfig) {
        var taxons = new ArrayList<Taxon>();
        var biofidTaxons = new ArrayList<BiofidTaxon>();

        JCasUtil.select(jCas, org.texttechnologylab.annotation.type.Taxon.class).forEach(t -> {
            var taxon = new Taxon(t.getBegin(), t.getEnd());
            taxon.setDocument(document);
            taxon.setValue(t.getValue());
            taxon.setCoveredText(t.getCoveredText());
            taxon.setIdentifier(t.getIdentifier());
            // We need to handle taxons specifically, depending on whether they have annotated identifiers.
            if (corpusConfig.getAnnotations().getTaxon().isBiofidOnthologyAnnotated() && taxon.getIdentifier() != null && !taxon.getIdentifier().isEmpty()) {
                // The recognized taxons should be split by a |
                var occurrences = new ArrayList<GbifOccurrence>();
                var splited = new ArrayList<String>();
                // Sometimes they are delimitered by |, sometimes by space - who knows in this dump? :)
                for (var split : taxon.getIdentifier().split("\\|")) {
                    splited.addAll(Arrays.asList(split.split(" ")));
                }

                for (var potentialBiofidId : splited) {
                    // The biofid urls are like: https://www.biofid.de/bio-ontologies/gbif/10428508
                    // We need the last number in that string, have a lookup into our sparql database and from there fetch the
                    // correct TaxonId
                    if (potentialBiofidId.isEmpty()) continue;

                    // Before we do GbifOccurence stuff, we build specific BiofidTaxon objects if we can.
                    var newBiofidTaxons = ExceptionUtils.tryCatchLog(
                            () -> jenaSparqlService.queryBiofidTaxon(potentialBiofidId),
                            (ex) -> logger.error("Error building a BiofidTaxon object from a potential id.", ex));
                    if (newBiofidTaxons != null) {
                        for (var biofidTaxon : newBiofidTaxons) {
                            biofidTaxon.setCoveredText(t.getCoveredText());
                            biofidTaxon.setBegin(t.getBegin());
                            biofidTaxon.setEnd(t.getEnd());
                            biofidTaxon.setDocument(document);
                            biofidTaxon.setBiofidUrl(potentialBiofidId);
                            biofidTaxons.add(biofidTaxon);
                        }
                    }

                    var taxonId = ExceptionUtils.tryCatchLog(
                            () -> jenaSparqlService.biofidIdUrlToGbifTaxonId(potentialBiofidId),
                            (ex) -> logger.error("Error getting the taxonId of a biofid annotation while importing.", ex));
                    if (taxonId == null || taxonId == -1) continue;
                    taxon.setGbifTaxonId(taxonId);

                    // Now check if we already have stored occurences for that taxon - we don't need to do that again then.
                    // We need to check in the current loop and in the database.
                    if (taxons.stream().anyMatch(ta -> ta.getGbifTaxonId() == taxonId)) break;
                    var is = ExceptionUtils.tryCatchLog(() -> db.checkIfGbifOccurrencesExist(taxonId),
                            (ex) -> logger.error("Error checking if taxon occurrence already exists.", ex));
                    if (is == null || is) break;

                    // Otherwise, fetch new occurrences.
                    var potentialOccurrences = ExceptionUtils.tryCatchLog(
                            () -> gbifService.scrapeGbifOccurrence(taxonId),
                            (ex) -> logger.error("Error scraping the gbif occurrence of taxonId: " + taxonId, ex));
                    if (potentialOccurrences != null && !potentialOccurrences.isEmpty()) {
                        occurrences.addAll(potentialOccurrences);
                        taxon.setPrimaryBiofidOntologyIdentifier(potentialBiofidId);
                        break;
                    }
                }
                taxon.setGbifOccurrences(occurrences);
            }
            taxons.add(taxon);
        });
        document.setTaxons(taxons);
        document.setBiofidTaxons(biofidTaxons);
        logger.info("Setting Taxons done.");
    }

    /**
     * Selects and sets the times to the document.
     */
    private void setTimes(Document document, JCas jCas) {
        var times = new ArrayList<Time>();
        JCasUtil.select(jCas, org.texttechnologylab.annotation.type.Time.class).forEach(t -> {
            var time = new Time(t.getBegin(), t.getEnd());
            time.setValue(t.getValue());
            time.setCoveredText(t.getCoveredText());

            // Let's see if we can dissect the raw time string into more usable formats for our db.
            var units = RegexUtils.DissectTimeAnnotationString(time.getCoveredText());
            time.setYear(units.year);
            time.setMonth(units.month);
            time.setDay(units.day);
            time.setDate(units.fullDate);
            time.setSeason(units.season);

            times.add(time);
        });
        document.setTimes(times);
        logger.info("Setting Times done.");
    }

    /**
     * Selects and sets the SRL to the document
     */
    private void setSemanticRoleLabels(Document document, JCas jCas) {
        var srLinks = new ArrayList<org.texttechnologylab.models.corpus.SrLink>();
        JCasUtil.select(jCas, org.texttechnologylab.annotation.semaf.semafsr.SrLink.class).forEach(a -> {
            var srLink = new org.texttechnologylab.models.corpus.SrLink();
            var figure = a.getFigure();
            var ground = a.getGround();
            srLink.setRelationType(a.getRel_type());

            srLink.setFigureBegin(figure.getBegin());
            srLink.setFigureEnd(figure.getEnd());
            srLink.setFigureCoveredText(figure.getCoveredText());

            srLink.setGroundBegin(ground.getBegin());
            srLink.setGroundEnd(ground.getEnd());
            srLink.setGroundCoveredText(ground.getCoveredText());

            srLinks.add(srLink);
        });
        document.setSrLinks(srLinks);
        logger.info("Setting Semantic-Roles done.");
    }

    /**
     * Selects and sets the lemmata to the document
     */
    private void setLemmata(Document document, JCas jCas) {
        // Set the lemmas
        var lemmas = new ArrayList<org.texttechnologylab.models.corpus.Lemma>();
        JCasUtil.select(jCas, de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma.class).forEach(l -> {
            var lemma = new org.texttechnologylab.models.corpus.Lemma(l.getBegin(), l.getEnd());
            lemma.setDocument(document);
            lemma.setCoveredText(l.getCoveredText());
            lemma.setValue(l.getValue());

            var potentialPos = JCasUtil.selectCovered(POS.class, l).stream().findFirst();
            if (potentialPos.isPresent()) {
                var pos = potentialPos.get();
                lemma.setPosValue(pos.getPosValue());
                lemma.setCoarseValue(pos.getCoarseValue());
            }

            var potentialMorph = JCasUtil.selectCovered(MorphologicalFeatures.class, l).stream().findFirst();
            if (potentialMorph.isPresent()) {
                var morph = potentialMorph.get();
                lemma.setAnimacy(morph.getAnimacy());
                lemma.setAspect(morph.getAspect());
                lemma.setCasee(morph.getCase());
                lemma.setDefiniteness(morph.getDefiniteness());
                lemma.setDegree(morph.getDegree());
                lemma.setGender(morph.getGender());
                lemma.setMood(morph.getMood());
                lemma.setNegative(morph.getNegative());
                lemma.setNumber(morph.getNumber());
                lemma.setNumberType(morph.getNumType());
                lemma.setPerson(morph.getPerson());
                lemma.setPossessive(morph.getPossessive());
                lemma.setPronType(morph.getPronType());
                lemma.setReflex(morph.getReflex());
                lemma.setTense(morph.getTense());
                lemma.setVerbForm(morph.getVerbForm());
                lemma.setVoice(morph.getVoice());
            }

            lemmas.add(lemma);
        });
        document.setLemmas(lemmas);
        logger.info("Setting Lemmas done.");
    }

    /**
     * Select and set the Named-Entities to the document
     */
    private void setNamedEntities(Document document, JCas jCas) {
        // Set the named entities
        var nes = new ArrayList<org.texttechnologylab.models.corpus.NamedEntity>();
        JCasUtil.select(jCas, de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity.class).forEach(ne -> {
            // We don't want all NE types
            if (ne == null || ne.getValue() == null) return;
            // We have different names for the types... sometimes they are full name, sometimes just the first three letters.
            var neType = "";
            for (var type : WANTED_NE_TYPES) {
                if (type.equals(ne.getValue()) || ne.getValue().equals(type.substring(0, 3))) neType = type;
            }
            if (neType.isEmpty()) return;

            var namedEntity = new org.texttechnologylab.models.corpus.NamedEntity(ne.getBegin(), ne.getEnd());
            namedEntity.setDocument(document);
            namedEntity.setType(neType);
            namedEntity.setCoveredText(ne.getCoveredText());
            nes.add(namedEntity);
        });
        document.setNamedEntities(nes);
        logger.info("Setting Named-Entities done.");
    }

    /**
     * Selects and sets the sentences to a document
     */
    private void setSentences(Document document, JCas jCas) {
        // Set the sentences
        document.setSentences(JCasUtil.select(jCas, de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence.class)
                .stream()
                .map(s -> new org.texttechnologylab.models.corpus.Sentence(s.getBegin(), s.getEnd(), s.getCoveredText()))
                .toList());
        logger.info("Setting sentences done.");
    }

    /**
     * Selects and sets CompleteNegations and all their dependent annotations
     */
    public void setCompleteNegations(Document document, JCas jCas) {
        // All Annotations
        ArrayList<CompleteNegation> cNegationsTotal = new ArrayList<>();
        ArrayList<Cue> cuesTotal = new ArrayList<>();
        ArrayList<Scope> scopesTotal = new ArrayList<>();
        ArrayList<XScope> xScopesTotal = new ArrayList<>();
        ArrayList<Focus> focusesTotal = new ArrayList<>();
        ArrayList<Event> eventsTotal = new ArrayList<>();

        //iterate over each negation
        for (org.texttechnologylab.annotation.negation.CompleteNegation negation : jCas.select(org.texttechnologylab.annotation.negation.CompleteNegation.class)) {
            // All target tokens
            Token cueT = negation.getCue();
            FSArray<Token> eventTL = negation.getEvent();
            FSArray<Token> scopeTL = negation.getScope();
            FSArray<Token> xscopeTL = negation.getXscope();
            FSArray<Token> focusTL = negation.getFocus();

            // annotations for one negation
            // -> partially set complete negation
            CompleteNegation cNegation = new CompleteNegation(cueT.getBegin(), cueT.getEnd());
            // -> fully set cue
            Cue cue = new Cue(cueT.getBegin(), cueT.getEnd());
            cue.setNegation(cNegation);
            cue.setDocument(document);
            cue.setCoveredText(cue.getCoveredText(jCas.getDocumentText()));

            // -> partially set scopes, xscopes, focuses and events
            ArrayList<Scope> scopes = new ArrayList<>();
            ArrayList<XScope> xScopes = new ArrayList<>();
            ArrayList<Focus> focuses = new ArrayList<>();
            ArrayList<Event> events = new ArrayList<>();
            if (eventTL != null) {
                ArrayList<ArrayList<Token>> spans = TokenUtils.findMaximalSpans(eventTL.stream().collect(Collectors.toCollection(ArrayList::new)));
                for (ArrayList<Token> span : spans) {
                    // -> fully set event
                    Event event = new Event(span.getFirst().getBegin(), span.getLast().getEnd());
                    event.setDocument(document);
                    event.setNegation(cNegation);
                    event.setCoveredText(event.getCoveredText(jCas.getDocumentText()));
                    events.add(event);
                }
            }
            if (scopeTL != null) {
                ArrayList<ArrayList<Token>> spans = TokenUtils.findMaximalSpans(scopeTL.stream().collect(Collectors.toCollection(ArrayList::new)));
                for (ArrayList<Token> span : spans) {
                    // -> fully set scope
                    Scope scope = new Scope(span.getFirst().getBegin(), span.getLast().getEnd());
                    scope.setDocument(document);
                    scope.setNegation(cNegation);
                    scope.setCoveredText(scope.getCoveredText(jCas.getDocumentText()));
                    scopes.add(scope);
                }
            }
            if (xscopeTL != null) {
                ArrayList<ArrayList<Token>> spans = TokenUtils.findMaximalSpans(xscopeTL.stream().collect(Collectors.toCollection(ArrayList::new)));
                for (ArrayList<Token> span : spans) {
                    // -> fully set xscope
                    XScope xscope = new XScope(span.getFirst().getBegin(), span.getLast().getEnd());
                    xscope.setDocument(document);
                    xscope.setNegation(cNegation);
                    xscope.setCoveredText(xscope.getCoveredText(jCas.getDocumentText()));
                    xScopes.add(xscope);
                }
            }
            if (focusTL != null) {
                ArrayList<ArrayList<Token>> spans = TokenUtils.findMaximalSpans(focusTL.stream().collect(Collectors.toCollection(ArrayList::new)));
                for (ArrayList<Token> span : spans) {
                    // -> fully set focus
                    Focus focus = new Focus(span.getFirst().getBegin(), span.getLast().getEnd());
                    focus.setDocument(document);
                    focus.setNegation(cNegation);
                    focus.setCoveredText(focus.getCoveredText(jCas.getDocumentText()));
                    focuses.add(focus);
                }
            }
            // fully set Negation
            cNegation.setDocument(document);
            cNegation.setCue(cue);
            cNegation.setEventList(events);
            cNegation.setScopeList(scopes);
            cNegation.setXscopeList(xScopes);
            cNegation.setFocusList(focuses);

            cNegationsTotal.add(cNegation);
            cuesTotal.add(cue);
            scopesTotal.addAll(scopes);
            xScopesTotal.addAll(xScopes);
            focusesTotal.addAll(focuses);
            eventsTotal.addAll(events);
        }
        document.setCompleteNegations(cNegationsTotal);
        document.setCues(cuesTotal);
        document.setScopes(scopesTotal);
        document.setXscopes(xScopesTotal);
        document.setFocuses(focusesTotal);
        document.setEvents(eventsTotal);
    }


    /**
     * Selects and sets the topics to a document
     */

    private void setUnifiedTopic(Document document, JCas jCas) {

        List<UnifiedTopic> unifiedTopics = new ArrayList<>();

        JCasUtil.select(jCas, org.texttechnologylab.annotation.UnifiedTopic.class).forEach(ut -> {
            UnifiedTopic unifiedTopic = new UnifiedTopic(ut.getBegin(), ut.getEnd());
            unifiedTopic.setDocument(document);

            if (ut.getTopics() != null) {
                List<TopicValueBase> topics = new ArrayList<>();
                for (org.texttechnologylab.annotation.TopicValueBase tvb : ut.getTopics().toArray(new org.texttechnologylab.annotation.TopicValueBase[0])) {
                    TopicValueBase topicValueBase;

                    // Check if the topic is of type TopicValueBaseWithScore
                    if (tvb instanceof org.texttechnologylab.annotation.TopicValueBaseWithScore) {
                        org.texttechnologylab.annotation.TopicValueBaseWithScore tvbWithScore = (org.texttechnologylab.annotation.TopicValueBaseWithScore) tvb;
                        TopicValueBaseWithScore topicValueBaseWithScore = new TopicValueBaseWithScore(ut.getBegin(), ut.getEnd());
                        topicValueBaseWithScore.setDocument(document);
                        topicValueBaseWithScore.setScore(tvbWithScore.getScore());
                        topicValueBase = topicValueBaseWithScore;
                    } else {
                        topicValueBase = new TopicValueBase(ut.getBegin(), ut.getEnd());
                        topicValueBase.setDocument(document);
                    }

                    topicValueBase.setValue(tvb.getValue());

                    if (tvb.getWords() != null) {
                        List<TopicWord> words = new ArrayList<>();
                        for (org.texttechnologylab.annotation.TopicWord tw : tvb.getWords().toArray(new org.texttechnologylab.annotation.TopicWord[0])) {
                            TopicWord topicWord = new TopicWord(tw.getBegin(), tw.getEnd());
                            topicWord.setWord(tw.getWord());
                            topicWord.setProbability(tw.getProbability());
                            topicWord.setTopic(topicValueBase);
                            topicWord.setCoveredText(topicWord.getCoveredText(jCas.getDocumentText()));
                            words.add(topicWord);
                        }
                        topicValueBase.setWords(words);
                    }
                    topicValueBase.setCoveredText(topicValueBase.getCoveredText(jCas.getDocumentText()));
                    topicValueBase.setUnifiedTopic(unifiedTopic);
                    topics.add(topicValueBase);
                }
                unifiedTopic.setTopics(topics);
            }

//            if (ut.getMetadata() != null) {
//                MetaData metadata = new MetaData();
//                metadata.setKey(ut.getMetadata().getKey());
//                metadata.setValue(ut.getMetadata().getValue());
//                unifiedTopic.setMetadata(metadata);
//            }
            unifiedTopic.setCoveredText(unifiedTopic.getCoveredText(jCas.getDocumentText()));

            unifiedTopics.add(unifiedTopic);
        });

        document.setUnifiedTopics(unifiedTopics);
    }

    /**
     * Set the cleaned full text. That is the sum of all tokens except of all anomalies
     * Update: OBSOLETE for now, as this takes forever and makes the OCR worse.
     */
    private void setCleanedFullText(Document document, JCas jCas) {
        var cleanedText = new StringJoiner(" ");
        JCasUtil.select(jCas, Token.class).forEach(t -> {
            // We don't want any tokens with suspicious chars here.
            if (t instanceof OCRToken ocr && ocr.getSuspiciousChars() > 0) {
                return;
            }
            var coveredAnomalies = JCasUtil.selectCovered(Anomaly.class, t).size();
            if (coveredAnomalies == 0) cleanedText.add(t.getCoveredText());
        });
        document.setFullTextCleaned(cleanedText.toString());
    }

    /**
     * Apply any postprocessing once the corpus is finished calculating. This will be called even
     * when the corpus import didn't finish due to an error. We still postprocess what we have.
     */
    private void postProccessCorpus(Corpus corpus, CorpusConfig corpusConfig) {
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
                                () -> ragService.getDocumentSentenceEmbeddingsOfDocument(d.getId()).stream(),
                                (ex) -> logger.error("Error getting the document sentence embeddings of document " + d.getId(), ex)))
                        .filter(Objects::nonNull)
                        .toList();

                // Now, from these sentences - generate a 2D and 3D tsne reduction embedding and store it
                // with the single document embedding
                var reducedSEmbeddingDto = ExceptionUtils.tryCatchLog(
                        () -> ragService.getEmbeddingDimensionReductions(
                                docSentenceEmbeddings.stream().map(DocumentSentenceEmbedding::getEmbedding).toList()),
                        (ex) -> logger.error("Error getting embedding dimension reductions in post processing a corpus.", ex));

                if (reducedSEmbeddingDto == null || reducedSEmbeddingDto.getTsne2D() == null) continue;
                // Store the tsne reduction in each sentence - this is basically now a 2D and 3D coordinate
                for (var i = 0; i < reducedSEmbeddingDto.getTsne2D().length; i++) {
                    docSentenceEmbeddings.get(i).setTsne2d(reducedSEmbeddingDto.getTsne2D()[i]);
                    docSentenceEmbeddings.get(i).setTsne3d(reducedSEmbeddingDto.getTsne3D()[i]);
                }
                // Update the changes (Could be a bulk Update... let's see :-)
                docSentenceEmbeddings.forEach(de -> ExceptionUtils.tryCatchLog(
                        () -> ragService.updateDocumentSentenceEmbedding(de),
                        (ex) -> logger.error("Error updating and saving a document sentence embedding.", ex)));


                // Get the complete list of document chunk embeddings of all documents
                var docChunkEmbeddings = documents.stream()
                        .flatMap(d -> ExceptionUtils.tryCatchLog(
                                () -> ragService.getDocumentChunkEmbeddingsOfDocument(d.getId()).stream(),
                                (ex) -> logger.error("Error getting the document chunk embeddings of document " + d.getId(), ex)))
                        .filter(Objects::nonNull)
                        .toList();

                // Now, from these chunks - generate a 2D and 3D tsne reduction embedding and store it
                // with the single document embedding
                var reducedEmbeddingDto = ExceptionUtils.tryCatchLog(
                        () -> ragService.getEmbeddingDimensionReductions(
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
                        () -> ragService.updateDocumentChunkEmbedding(de),
                        (ex) -> logger.error("Error updating and saving a document chunk embedding.", ex)));

                // And calculate a reduced embedding for the whole document as well!
                for (var document : documents) {
                    var documentEmbedding = ExceptionUtils.tryCatchLog(
                            () -> ragService.getDocumentEmbeddingOfDocument(document.getId()),
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
                            () -> ragService.updateDocumentEmbedding(documentEmbedding),
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

    /**
     * Here we apply any postprocessing of a document that isn't DUUI and needs the document to be stored once like
     * the rag vector embeddings.
     */
    private void postProccessDocument(Document document, Corpus corpus, String filePath) {
        logImportInfo("Postprocessing " + filePath, LogStatus.POST_PROCESSING, filePath, 0);
        var start = System.currentTimeMillis();
        var corpusConfig = corpus.getViewModel().getCorpusConfig();

        // Calculate embeddings if they are activated
        if (corpusConfig.getOther().isEnableEmbeddings()) {
            logger.info("Embeddings...");

            // Sentence Embeddings
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
            }

            // Chunk Embeddings
            var docHasChunkEmbeddings = ExceptionUtils.tryCatchLog(
                    () -> ragService.documentHasDocumentChunkEmbeddings(document.getId()),
                    (ex) -> logImportError("Error while checking if a document already has DocumentChunkEmbeddings.", ex, filePath));
            if (docHasChunkEmbeddings != null && !docHasChunkEmbeddings) {
                // Build the chunks, which are the most crucial embeddings
                var documentChunkEmbeddings = ExceptionUtils.tryCatchLog(
                        () -> ragService.getCompleteEmbeddingChunksFromDocument(document),
                        (ex) -> logImportError("Error getting the complete embedding chunks for document: " + document.getId(), ex, filePath));

                // Store the chunks
                if (documentChunkEmbeddings != null)
                    for (var docEmbedding : documentChunkEmbeddings) {
                        ExceptionUtils.tryCatchLog(
                                () -> ragService.saveDocumentChunkEmbedding(docEmbedding),
                                (ex) -> logImportError("Error saving a document chunk embeddings.", ex, filePath)
                        );
                    }
            }

            // Document Embedding
            var docHasEmbedding = ExceptionUtils.tryCatchLog(
                    () -> ragService.documentHasDocumentEmbedding(document.getId()),
                    (ex) -> logImportError("Error while checking if a document already has a DocumentEmbedding.", ex, filePath));
            if (docHasEmbedding != null && !docHasEmbedding) {
                // Build a single document embeddings for the whole text
                var documentEmbedding = ExceptionUtils.tryCatchLog(
                        () -> ragService.getCompleteEmbeddingFromDocument(document),
                        (ex) -> logImportError("Error getting the complete embedding from a document.", ex, filePath));

                // Store the single document embedding
                if (documentEmbedding != null)
                    ExceptionUtils.tryCatchLog(
                            () -> ragService.saveDocumentEmbedding(documentEmbedding),
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
                        () -> ragService.getTextKeywordDistribution(PageKeywordDistribution.class, page.getCoveredText(document.getFullText())),
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
                        () -> ragService.getTextKeywordDistribution(DocumentKeywordDistribution.class, document.getFullText()),
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
    }

    /**
     * Gets all covered lines from a OCR page in a cas
     */
    private List<Line> getCoveredLines(org.texttechnologylab.annotation.ocr.abbyy.Page page) {
        // Paragraphs
        var lines = new ArrayList<Line>();
        // Get all covered by this. This can probably be done in one go, but oh well
        JCasUtil.selectCovered(OCRLine.class, page).forEach(pg -> {
            var line = new Line(pg.getBegin(), pg.getEnd());
            line.setBaseline(pg.getBaseline());
            line.setBottom(pg.getBottom());
            line.setLeft(pg.getLeft());
            line.setTop(pg.getTop());
            line.setRight(pg.getRight());

            lines.add(line);
        });
        return lines;
    }

    /**
     * Gets all covered blocks from a OCR page in a cas
     */
    private List<Block> getCoveredBlocks(org.texttechnologylab.annotation.ocr.abbyy.Page page) {
        // Paragraphs
        var blocks = new ArrayList<Block>();
        // Get all covered by this. This can probably be done in one go, but oh well
        JCasUtil.selectCovered(OCRBlock.class, page).forEach(pg -> {
            var block = new Block(pg.getBegin(), pg.getEnd());
            block.setBlockType(pg.getBlockType());
            blocks.add(block);
        });
        return blocks;
    }

    /**
     * Gets all covered paragraphs from a OCR page in a cas
     */
    private List<Paragraph> getCoveredParagraphs(org.texttechnologylab.annotation.ocr.abbyy.Page page) {
        // Paragraphs
        var paragraphs = new ArrayList<Paragraph>();
        // Get all covered by this. This can probably be done in one go, but oh well
        JCasUtil.selectCovered(org.texttechnologylab.annotation.ocr.abbyy.Paragraph.class, page).forEach(pg -> {
            var paragraph = new Paragraph(pg.getBegin(), pg.getEnd());
            //paragraph.setAlign(pg.getAlign());
            paragraph.setLeftIndent(pg.getLeftIndent());
            paragraph.setLineSpacing(pg.getLineSpacing());
            paragraph.setRightIndent(pg.getRightIndent());
            paragraph.setStartIndent(pg.getStartIndent());
            paragraph.setCoveredText(pg.getCoveredText());

            paragraphs.add(paragraph);
        });
        return paragraphs;
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
    private void logImportWarn(String message, Exception ex, String file) {
        var importLog = new ImportLog(this.importerNumber.toString(), ex.getMessage(), LogStatus.WARN, file, this.importId, 0);
        tryStoreUCEImportLog(importLog);
        logger.warn(message, ex);
    }

    /**
     * Not only logs to the default file logger, but also as a special ImportLog into the database.
     */
    private void logImportError(String message, Exception ex, String file) {
        var importLog = new ImportLog(this.importerNumber.toString(), ex.getMessage(), LogStatus.ERROR, file, this.importId, 0);
        tryStoreUCEImportLog(importLog);
        logger.error(message, ex);
    }

}
