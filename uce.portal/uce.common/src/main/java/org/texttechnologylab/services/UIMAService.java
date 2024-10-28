package org.texttechnologylab.services;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.Anomaly;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import io.kubernetes.client.Exec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.util.CasLoadMode;
import org.joda.time.DateTime;
import org.springframework.dao.DataAccessException;
import org.texttechnologylab.annotation.DocumentAnnotation;
import org.texttechnologylab.annotation.semaf.semafsr.SrLink;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.springframework.stereotype.Service;
import org.texttechnologylab.annotation.ocr.*;
import org.texttechnologylab.config.CorpusConfig;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.rag.DocumentChunkEmbedding;
import org.texttechnologylab.models.util.HealthStatus;
import org.texttechnologylab.utils.EmbeddingUtils;
import org.texttechnologylab.utils.ListUtils;
import org.texttechnologylab.utils.SystemStatus;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
/*
Holds logic for interacting with any UIMA associated data. The UIMAService is the ONLY service that catches its own exceptions and adds logging!
 */
public class UIMAService {
    private static final Logger logger = LogManager.getLogger();
    private static final Set<String> WANTED_NE_TYPES = Set.of(
            "LOCATION", "MISC", "PERSON", "ORGANIZATION"
    );
    private GoetheUniversityService goetheUniversityService;
    private PostgresqlDataInterface_Impl db;
    private GbifService gbifService;
    private RAGService ragService;
    private JenaSparqlService jenaSparqlService;

    public UIMAService(GoetheUniversityService goetheUniversityService,
                       PostgresqlDataInterface_Impl db,
                       GbifService gbifService,
                       RAGService ragService,
                       JenaSparqlService jenaSparqlService) {
        try {
            this.goetheUniversityService = goetheUniversityService;
            this.db = db;
            this.ragService = ragService;
            this.jenaSparqlService = jenaSparqlService;
            this.gbifService = gbifService;
            SystemStatus.UIMAService = new HealthStatus(true, "", null);
        } catch (Exception ex) {
            SystemStatus.UIMAService = new HealthStatus(false, "Error initing the service", ex);
        }
    }

    /**
     * Imports all UIMA xmi files in a folder
     *
     * @param foldername
     * @return
     */
    public void storeCorpusFromFolder(String foldername) throws DatabaseOperationException {
        var corpus = new Corpus();
        var gson = new Gson();
        CorpusConfig corpusConfig = null;

        if (!SystemStatus.PostgresqlDbStatus.isAlive())
            throw new DatabaseOperationException("Postgresql DB is not alive - cancelling import.");

        // Read the corpus config. If this doesn't exist, we cannot import the corpus
        try (var reader = new FileReader(foldername + "\\corpusConfig.json")) {

            // Replace "path/to/your/file.json" with the actual path to your JSON file
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

        var counter = 0;
        final var corpusConfigFinal = corpusConfig;
        for (var file : Objects.requireNonNull(
                new File(foldername)
                        .listFiles((dir, name) -> name.toLowerCase().endsWith(".xmi")))) {
            var doc = XMIToDocument(file.getPath(), corpus);
            if (doc != null) {
                // Save it
                ExceptionUtils.tryCatchLog(
                        () -> db.saveDocument(doc),
                        (ex) -> logger.error("Error saving a finished document with id " + doc.getId()));
                logger.info("Stored document with document id " + doc.getDocumentId());
                logger.info("Finished with the UIMA annotations - postprocessing the doc now.");

                // Now eventually do postprocessing of the document
                ExceptionUtils.tryCatchLog(
                        () -> postProccessDocument(doc, corpusConfigFinal),
                        (ex) -> logger.error("Error postprocessing a saved document with id " + doc.getId()));
                logger.info("Finished postprocessing.");
            }

            // We occasionally postprocess the corpus while we still import to keep it up to date
            if (counter % 100 == 0 && counter != 0) {
                final var finalCorpus = corpus;
                ExceptionUtils.tryCatchLog(
                        () -> postProccessCorpus(finalCorpus, corpusConfigFinal),
                        (ex) -> logger.error("Error postprocessing the current corpus with id " + finalCorpus.getId()));
            }
            counter++;
        }

        // At the end, postprocess the corpus
        final var finalCorpus = corpus;
        ExceptionUtils.tryCatchLog(
                () -> postProccessCorpus(finalCorpus, corpusConfigFinal),
                (ex) -> logger.error("Error in the final postprocessing of the current corpus with id " + finalCorpus.getId()));

        logger.info("\n\n=================================\n Done with the corpus import.");
    }

    /**
     * Converts an XMI to an OCRDocument by path
     *
     * @param filename
     * @return
     */
    public Document XMIToDocument(String filename, Corpus corpus) {
        try {
            var jCas = JCasFactory.createJCas();
            // Read in the contents of a single xmi cas
            //var file = new GZIPInputStream(new FileInputStream(filename));
            var file = new FileInputStream(filename);
            // https://uima.apache.org/d/uimaj-current/api/org/apache/uima/util/CasIOUtils.html
            // tsiInputStream: Optional stream for typesystem - only used if not null. (which it currently is)
            CasIOUtils.load(file, null, jCas.getCas(), CasLoadMode.LENIENT);

            return XMIToDocument(jCas, corpus);
        } catch (Exception ex) {
            logger.error("Error while reading an annotated xmi file to a cas and transforming it into a document:", ex);
            return null;
        }
    }

    /**
     * Convert a UIMA jCas to an OCRDocument
     *
     * @param jCas
     * @return
     */
    public Document XMIToDocument(JCas jCas, Corpus corpus) {

        logger.info("\n ======== Importing a new CAS as a Document.");
        // Read in the contents of a single xmi cas to see what's inside
        var unique = new HashSet<String>();
        JCasUtil.select(jCas, AnnotationBase.class).forEach(a -> {
            unique.add(a.getType().getName());
        });
        unique.forEach(logger::info);

        try {
            // Corpus config so we now what do look for
            var gson = new Gson();
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

            // Before we parse and add that document, lets check if a document with that id and in that
            // corpus already exists. If we created a new corpus, this will always be null.
            var exists = db.documentExists(corpus.getId(), document.getDocumentId());
            if (exists) {
                logger.info("Document with id " + document.getDocumentId()
                        + " already exists in the corpus " + corpus.getId() + ". Skipping it.");
                return null;
            }

            // Set the full text
            document.setFullText(jCas.getDocumentText());
            logger.info("Setting full text done.");

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
                var documentAnnotation = JCasUtil.selectSingle(jCas, DocumentAnnotation.class);
                try {
                    metadataTitleInfo.setPublished(documentAnnotation.getDateDay() + "."
                            + documentAnnotation.getDateMonth() + "."
                            + documentAnnotation.getDateYear());
                } catch (Exception ex) {
                    logger.warn("Tried extracting DocumentAnnotation type, it caused an error. Import will be continued as usual.");
                }
            }
            document.setMetadataTitleInfo(metadataTitleInfo);

            // Set the cleaned full text. That is the sum of all tokens except of all anomalies
            // For now, we skip this. This doesn't relly improve anything and is very costly.
            if (false) {
                var cleanedText = new StringJoiner(" ");
                JCasUtil.select(jCas, Token.class).forEach(t -> {
                    // We dont want any tokens with suspicous chars here.
                    if (t instanceof OCRToken ocr && ocr.getSuspiciousChars() > 0) {
                        return;
                    }
                    var coveredAnomalies = JCasUtil.selectCovered(Anomaly.class, t).size();
                    if (coveredAnomalies == 0) cleanedText.add(t.getCoveredText());
                });
                document.setFullTextCleaned(cleanedText.toString());
            }

            // Set the sentences
            if (corpusConfig.getAnnotations().isSentence()) {
                document.setSentences(JCasUtil.select(jCas, de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence.class)
                        .stream()
                        .map(s -> new org.texttechnologylab.models.corpus.Sentence(s.getBegin(), s.getEnd()))
                        .toList());
                logger.info("Setting sentences done.");
            }

            // Set the named entities
            if (corpusConfig.getAnnotations().isNamedEntity()) {
                var nes = new ArrayList<org.texttechnologylab.models.corpus.NamedEntity>();
                JCasUtil.select(jCas, NamedEntity.class).forEach(ne -> {
                    // We don't want all NE types
                    if (ne == null || ne.getValue() == null) return;
                    // We have different names for the types... sometimes they are full name, sometimes just the first three letters.
                    var neType = "";
                    for (var type : WANTED_NE_TYPES) {
                        if (type.equals(ne.getValue()) || ne.getValue().equals(type.substring(0, 3))) neType = type;
                    }
                    if (neType.isEmpty()) return;

                    var namedEntity = new org.texttechnologylab.models.corpus.NamedEntity(ne.getBegin(), ne.getEnd());
                    namedEntity.setType(neType);
                    namedEntity.setCoveredText(ne.getCoveredText());
                    nes.add(namedEntity);
                });
                document.setNamedEntities(nes);
                logger.info("Setting Named-Entities done.");
            }

            // Set the lemmas
            if (corpusConfig.getAnnotations().isLemma()) {
                var lemmas = new ArrayList<org.texttechnologylab.models.corpus.Lemma>();
                JCasUtil.select(jCas, de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma.class).forEach(l -> {
                    var lemma = new org.texttechnologylab.models.corpus.Lemma(l.getBegin(), l.getEnd());
                    lemma.setCoveredText(l.getCoveredText());
                    lemma.setValue(l.getValue());
                    lemmas.add(lemma);
                });
                document.setLemmas(lemmas);
                logger.info("Setting Lemmas done.");
            }

            // Set the semantic role labels
            if (corpusConfig.getAnnotations().isSrLink()) {
                var srLinks = new ArrayList<org.texttechnologylab.models.corpus.SrLink>();
                JCasUtil.select(jCas, SrLink.class).stream().forEach(a -> {
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

            // Set the times
            if (corpusConfig.getAnnotations().isTime()) {
                var times = new ArrayList<Time>();
                JCasUtil.select(jCas, org.texttechnologylab.annotation.type.Time.class).forEach(t -> {
                    var time = new Time(t.getBegin(), t.getEnd());
                    time.setValue(t.getValue());
                    time.setCoveredText(t.getCoveredText());
                    times.add(time);
                });
                document.setTimes(times);
                logger.info("Setting Times done.");
            }

            // Set the taxons
            if (corpusConfig.getAnnotations().getTaxon().isAnnotated()) {
                var taxons = new ArrayList<Taxon>();
                JCasUtil.select(jCas, org.texttechnologylab.annotation.type.Taxon.class).forEach(t -> {
                    var taxon = new Taxon(t.getBegin(), t.getEnd());
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
                logger.info("Setting Taxons done.");
            }

            // Wikipedia/Wikidata?
            if (corpusConfig.getAnnotations().isWikipediaLink()) {
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

            // Set the OCRpages
            if (corpusConfig.getAnnotations().isOCRPage()) {
                var pages = new ArrayList<Page>();
                // We go through each page
                JCasUtil.select(jCas, OCRPage.class).forEach(p -> {
                    // New page
                    var page = new Page(p.getBegin(), p.getEnd(), p.getPageNumber(), p.getPageId());
                    if (corpusConfig.getAnnotations().isOCRParagraph())
                        page.setParagraphs(getCoveredParagraphs(p));

                    if (corpusConfig.getAnnotations().isOCRBlock())
                        page.setBlocks(getCoveredBlocks(p, jCas));

                    if (corpusConfig.getAnnotations().isOCRLine())
                        page.setLines(getCoveredLines(p, jCas));

                    pages.add(page);
                });
                document.setPages(pages);
                logger.info("Setting OCRPages done.");
            } else {
                // If the corpus isn't OCRPage annotated, we create our own pseudo pages.
                // We want pages as our pagination of the document reader relies on it to handle larger documents.
                // In this case: we chunk the whole text into pages
                var fullText = document.getFullText();
                var pageSize = 6000;
                var pageNumber = 1;
                var pages = new ArrayList<Page>();

                for (var i = 0; i < fullText.length(); i += pageSize) {
                    var page = new Page(i, i + pageSize, pageNumber, "");
                    pageNumber += 1;
                    pages.add(page);
                }
                document.setPages(pages);
                logger.info("Setting synthetic pages done.");
            }

            return document;
        } catch (Exception ex) {
            logger.error("Unknown error while importing a CAS into a document. This shouldn't happen, as each operation has its own error handling.", ex);
            return null;
        }
    }

    /**
     * Apply any postprocessing once the corpus is finished calculating. This will be called even
     * when the corpus import didn't finish due to an error. We still postprocess what we have.
     *
     * @param corpus
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

            logger.info("Corpus TSNE Plot...");

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
                    (ex) -> logger.error("Error saving or updating the corpus tsne plot.", ex));
        }
        logger.info("Done with the corpus postprocessing.");
    }

    /**
     * Here we apply any post processing of a document that isn't DUUI and needs the document to be stored once like
     * the rag vector embeddings
     */
    private void postProccessDocument(Document document, CorpusConfig corpusConfig) {
        logger.info("Postprocessing the document: " + document.getId());

        // Calculate embeddings if they are activated
        if (corpusConfig.getOther().isEnableEmbeddings()) {
            logger.info("embeddings...");
            // Build the chunks, which are the most crucial embeddings
            var documentChunkEmbeddings = ExceptionUtils.tryCatchLog(
                    () -> ragService.getCompleteEmbeddingChunksFromDocument(document),
                    (ex) -> logger.error("Error getting the complete embedding chunks for document: " + document.getId(), ex));

            // Build a single document embeddings for the whole text
            var documentEmbedding = ExceptionUtils.tryCatchLog(
                    () -> ragService.getCompleteEmbeddingFromDocument(document),
                    (ex) -> logger.error("Error getting the complete embedding from a document.", ex));

            // Store the single document embedding
            if (documentEmbedding != null)
                ExceptionUtils.tryCatchLog(
                        () -> ragService.saveDocumentEmbedding(documentEmbedding),
                        (ex) -> logger.error("Error saving a document embedding.", ex));

            // Store the chunks
            if (documentChunkEmbeddings != null)
                for (var docEmbedding : documentChunkEmbeddings) {
                    ExceptionUtils.tryCatchLog(
                            () -> ragService.saveDocumentChunkEmbedding(docEmbedding),
                            (ex) -> logger.error("Error saving a document chunk embeddings.", ex)
                    );
                }
        }

        if (corpusConfig.getOther().isIncludeTopicDistribution()) {
            logger.info("topic distribution...");

            // Calculate the page topic distribution if activated
            for (var page : document.getPages()) {
                var topicDistribution = ExceptionUtils.tryCatchLog(
                        () -> ragService.getTextTopicDistribution(PageTopicDistribution.class, page.getCoveredText(document.getFullText())),
                        (ex) -> logger.error("Error getting the PageTopicDistribution - the postprocessing continues. Document id: " + document.getId(), ex));
                if (topicDistribution == null) continue;

                topicDistribution.setBegin(page.getBegin());
                topicDistribution.setEnd(page.getEnd());
                topicDistribution.setPage(page);
                page.setPageTopicDistribution(topicDistribution);
                // Store it in the db
                ExceptionUtils.tryCatchLog(() -> db.savePageTopicDistribution(page),
                        (ex) -> logger.error("Error storing the page topic distribution - the postprocessing continues.", ex));
            }

            // And the document topic dist.
            var documentTopicDistribution = ExceptionUtils.tryCatchLog(
                    () -> ragService.getTextTopicDistribution(DocumentTopicDistribution.class, document.getFullText()),
                    (ex) -> logger.error("Error getting the DocumentTopicDistribution - the postprocessing ends now. Document id: " + document.getId(), ex));
            if (documentTopicDistribution == null) return;

            documentTopicDistribution.setDocument(document);
            document.setDocumentTopicDistribution(documentTopicDistribution);
            // Store it
            ExceptionUtils.tryCatchLog(() -> db.saveDocumentTopicDistribution(document),
                    (ex) -> logger.error("Error storing the document topic distribution - the postprocessing ends now.", ex));
        }
    }

    /**
     * Gets all covered lines from a OCR page in a cas
     *
     * @param page
     * @param jCas
     * @return
     */
    private List<Line> getCoveredLines(OCRPage page, JCas jCas) {
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
     *
     * @param page
     * @param jCas
     * @return
     */
    private List<Block> getCoveredBlocks(OCRPage page, JCas jCas) {
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
     *
     * @param page
     * @return
     */
    private List<Paragraph> getCoveredParagraphs(OCRPage page) {
        // Paragraphs
        var paragraphs = new ArrayList<Paragraph>();
        // Get all covered by this. This can probably be done in one go, but oh well
        JCasUtil.selectCovered(OCRParagraph.class, page).forEach(pg -> {
            var paragraph = new Paragraph(pg.getBegin(), pg.getEnd());
            paragraph.setAlign(pg.getAlign());
            paragraph.setLeftIndent(pg.getLeftIndent());
            paragraph.setLineSpacing(pg.getLineSpacing());
            paragraph.setRightIndent(pg.getRightIndent());
            paragraph.setStartIndent(pg.getStartIndent());
            paragraph.setCoveredText(pg.getCoveredText());

            paragraphs.add(paragraph);
        });
        return paragraphs;
    }

}
