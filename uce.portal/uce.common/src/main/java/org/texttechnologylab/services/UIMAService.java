package org.texttechnologylab.services;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.Anomaly;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.util.CasLoadMode;
import org.joda.time.DateTime;
import org.texttechnologylab.annotation.semaf.semafsr.SrLink;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.springframework.stereotype.Service;
import org.texttechnologylab.annotation.ocr.*;
import org.texttechnologylab.config.CorpusConfig;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.rag.DocumentChunkEmbedding;
import org.texttechnologylab.utils.EmbeddingUtils;
import org.texttechnologylab.utils.ListUtils;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
/*
TODO: Add logging!
Holds logic for interacting with any UIMA associated data
 */
public class UIMAService {

    private static final Set<String> WANTED_NE_TYPES = Set.of(
            "LOCATION", "MISC", "PERSON", "ORGANIZATION"
    );
    private final GoetheUniversityService goetheUniversityService;
    private final PostgresqlDataInterface_Impl db;
    private final GbifService gbifService;
    private final RAGService ragService;

    public UIMAService(GoetheUniversityService goetheUniversityService,
                       PostgresqlDataInterface_Impl db,
                       GbifService gbifService,
                       RAGService ragService) {
        this.goetheUniversityService = goetheUniversityService;
        this.db = db;
        this.ragService = ragService;
        this.gbifService = gbifService;
    }

    /**
     * Imports all UIMA xmi files in a folder
     *
     * @param foldername
     * @return
     */
    public void storeCorpusFromFolder(String foldername) {
        var corpus = new Corpus();
        var gson = new Gson();
        CorpusConfig corpusConfig = null;

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
                var existingCorpus = db.getCorpusByName(corpusConfig.getName());
                if (existingCorpus != null) { // If we have the corpus, use that. Else store the new corpus.
                    corpus = existingCorpus;
                } else {
                    db.saveCorpus(corpus);
                }
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            throw new MissingResourceException(
                    "The corpus folder did not contain a properly formatted corpusConfig.json", CorpusConfig.class.toString(), "");
        }

        var counter = 0;
        for (var file : Objects.requireNonNull(
                new File(foldername)
                        .listFiles((dir, name) -> name.toLowerCase().endsWith(".xmi")))) {
            var doc = XMIToDocument(file.getPath(), corpus);
            if (doc != null) {
                try{
                    db.saveDocument(doc);
                    System.out.println("Stored document with document id " + doc.getDocumentId());
                    System.out.println("Finished with the UIMA annotations - postprocessing the doc now.");
                    postProccessDocument(doc, corpusConfig);
                    System.out.println("Finished postprocessing.");

                    // We occasionally postprocess the corpus while we still import to keep it up to date
                    if(counter % 500 == 0 && counter != 0){
                        postProccessCorpus(corpus, corpusConfig);
                    }
                } catch (Exception ex){
                    System.err.println("Error storing a finished document. Skipping it and going to the next.");
                    ex.printStackTrace();
                }
            }
            counter++;
        }

        // At the end, postprocess the corpus
        postProccessCorpus(corpus, corpusConfig);
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
            // TODO: Log properly here.
            System.err.println("Error while reading a xmi file to a cas:");
            ex.printStackTrace();
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

        // Read in the contents of a single xmi cas to see what's inside
        var unique = new HashSet<String>();
        JCasUtil.select(jCas, AnnotationBase.class).stream().forEach(a -> {
            unique.add(a.getType().getName());
        });
        unique.forEach(System.out::println);

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

            // Before we parse and add that document, lets check if a document with that id and in that
            // corpus already exists. If we created a new corpus, this will always be null.
            var exists = db.documentExists(corpus.getId(), document.getDocumentId());
            if(exists){
                System.out.println("Document with id " + document.getDocumentId()
                        + " already exists in the corpus " + corpus.getId() + ". Skipping it.");
                return null;
            }

            // Set the full text
            document.setFullText(jCas.getDocumentText());

            // See if we can get any more informatiom from the goethe collections
            document.setMetadataTitleInfo(new MetadataTitleInfo());
            if (corpusConfig.getOther().isAvailableOnFrankfurtUniversityCollection())
                document.setMetadataTitleInfo(goetheUniversityService.scrapeDocumentTitleInfo(document.getDocumentId()));

            // Set the cleaned full text. That is the sum of all tokens except of all anomalies
            // TODO: For now, we skip this. This doesn't relly improve anything and is very costly.
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
            if (corpusConfig.getAnnotations().isSentence())
                document.setSentences(JCasUtil.select(jCas, de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence.class)
                        .stream()
                        .map(s -> new org.texttechnologylab.models.corpus.Sentence(s.getBegin(), s.getEnd()))
                        .toList());

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
                    if (neType.equals("")) return;

                    var namedEntity = new org.texttechnologylab.models.corpus.NamedEntity(ne.getBegin(), ne.getEnd());
                    namedEntity.setType(neType);
                    namedEntity.setCoveredText(ne.getCoveredText());
                    nes.add(namedEntity);
                });
                document.setNamedEntities(nes);
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
            }

            // Set the taxons
            if (corpusConfig.getAnnotations().getTaxon().isAnnotated()) {
                var taxons = new ArrayList<Taxon>();
                JCasUtil.select(jCas, org.texttechnologylab.annotation.type.Taxon.class).forEach(t -> {
                    var taxon = new Taxon(t.getBegin(), t.getEnd());
                    taxon.setValue(t.getValue());
                    taxon.setCoveredText(t.getCoveredText());
                    taxon.setIdentifier(t.getIdentifier());
                    // We need to handle taxons specifically, depending if they have annotated identifiers.
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
                            // We need the last number in that string, have a lookup into our sparsql database and from there fetch the
                            // correct TaxonId
                            if (potentialBiofidId.isEmpty()) continue;

                            var taxonId = gbifService.biofidIdUrlToGbifTaxonId(potentialBiofidId);
                            if (taxonId == -1) continue;
                            taxon.setGbifTaxonId(taxonId);

                            // Now check if we already have stored occurences for that taxon - we don't need to do that again then.
                            // We need to check in the current loop and in the database.
                            if (taxons.stream().anyMatch(ta -> ta.getGbifTaxonId() == taxonId)) break;
                            if (db.checkIfGbifOccurrencesExist(taxonId)) break;

                            // Otherwise, fetch new occurrences.
                            var potentialOccurrences = gbifService.scrapeGbifOccurrence(taxonId);
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
            }

            // Set the OCRpages
            if (corpusConfig.getAnnotations().isOCRPage()) {
                var pages = new ArrayList<Page>();
                // We go through each page
                JCasUtil.select(jCas, OCRPage.class).forEach(p -> {
                    // New page
                    var page = new Page(p.getBegin(), p.getEnd(), p.getPageNumber(), p.getPageId());
                    if (corpusConfig.getAnnotations().isOCRParagraph())
                        page.setParagraphs(getCoveredParagraphs(p, jCas));

                    if (corpusConfig.getAnnotations().isOCRBlock())
                        page.setBlocks(getCoveredBlocks(p, jCas));

                    if (corpusConfig.getAnnotations().isOCRLine())
                        page.setLines(getCoveredLines(p, jCas));

                    pages.add(page);
                });
                document.setPages(pages);
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
            }

            return document;
        } catch (Exception ex) {
            // TODO: Log properly here.
            System.err.println("Error while importing corpus:");
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Apply any postprocessing once the corpus is finished calculating. This will be called even
     * when the corpus import didnt finish due to an error. We still postprocess what we have.
     * @param corpus
     */
    private void postProccessCorpus(Corpus corpus, CorpusConfig corpusConfig){
        System.out.println("Postprocessing the Corpus " + corpus.getName());
        // Calculate the tsne reductions of the whole corpus and finally the tsne plot
        if(corpusConfig.getOther().isEnableEmbeddings()){
            System.out.println("Embeddings...");

            // The corpus can be gigantic and we cant pass hundreds of thousand of embeddings into
            // a rest API and perform reductions on them. Instead, we sample them if we need.
            var corpusDocuments = db.getNonePostprocessedDocumentsByCorpusId(corpus.getId());
            Collections.shuffle(corpusDocuments); // We want random samples of size CHUNKSIZE
            var chunked = ListUtils.partitionList(corpusDocuments, 1000);

            for(var documents:chunked){
                // Get the complete list of document chunk embeddings of all documents
                var docChunkEmbeddings = documents.stream()
                        .flatMap(d -> ragService.getDocumentChunkEmbeddingsOfDocument(d.getId()).stream())
                        .toList();

                // Now, from these chunks - generate a 2D and 3D tsne reduction embedding and store it
                // with the single document embedding
                var reducedEmbeddingDto = ragService.getEmbeddingDimensionReductions(
                        docChunkEmbeddings.stream().map(DocumentChunkEmbedding::getEmbedding).toList());

                if(reducedEmbeddingDto.getTsne2D() == null) continue;
                // Store the tsne reduction in each chunk - this is basically now a 2D and 3D coordinate
                for(var i = 0; i < reducedEmbeddingDto.getTsne2D().length; i++){
                    docChunkEmbeddings.get(i).setTsne2D(reducedEmbeddingDto.getTsne2D()[i]);
                    docChunkEmbeddings.get(i).setTsne3D(reducedEmbeddingDto.getTsne3D()[i]);
                }
                // Update the changes (Could be a bulk Update... let's see :-)
                docChunkEmbeddings.forEach(ragService::updateDocumentChunkEmbedding);

                // And calculate a reduced embedding for the whole document as well!
                for(var document:documents){
                    var documentEmbedding = ragService.getDocumentEmbeddingOfDocument(document.getId());
                    if(documentEmbedding == null) continue;
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
                    db.updateDocument(document);

                    // Update the document embedding
                    ragService.updateDocumentEmbedding(documentEmbedding);
                }
            }

            System.out.println("Corpus TSNE Plot...");

            // Now that we have the reduced coordiantes, lets plot a tsne plot of the corpus and cache it!
            // If we have an existing plot, then update that
            var corpusTsnePlot = corpus.getCorpusTsnePlot();
            if(corpusTsnePlot == null) corpusTsnePlot = new CorpusTsnePlot();
            corpusTsnePlot.setPlotHtml(ragService.getCorpusTsnePlot(corpus.getId()));
            corpusTsnePlot.setCorpus(corpus);
            corpusTsnePlot.setCreated(DateTime.now().toDate());

            corpus.setCorpusTsnePlot(corpusTsnePlot);
            db.saveOrUpdateCorpusTsnePlot(corpusTsnePlot, corpus);
        }
    }

    /**
     * Here we apply any post processing of a document that isn't DUUI and needs the document to be stored once like
     * the rag vector embeddings
     */
    private void postProccessDocument(Document document, CorpusConfig corpusConfig) {
        // Calculate embeddings if they are activated
        if (corpusConfig.getOther().isEnableEmbeddings()) {
            // Build the chunks, which are the most crucial embeddings
            var documentChunkEmbeddings = ragService.getCompleteEmbeddingChunksFromDocument(document);
            // Build a single document embeddings for the whole text
            var documentEmbedding = ragService.getCompleteEmbeddingFromDocument(document);

            // Store the single document embedding
            ragService.saveDocumentEmbedding(documentEmbedding);

            // Store the chunks
            for (var docEmbedding : documentChunkEmbeddings) {
                ragService.saveDocumentChunkEmbedding(docEmbedding);
            }
        }

        if(corpusConfig.getOther().isIncludeTopicDistribution()){
            // Calculate the page topic distribution if activated
            var pageTopics = "";
            for(var page: document.getPages()){
                var topicDistribution = ragService.getTextTopicDistribution(PageTopicDistribution.class, page.getCoveredText(document.getFullText()));
                topicDistribution.setBegin(page.getBegin());
                topicDistribution.setEnd(page.getEnd());
                topicDistribution.setPage(page);
                pageTopics += topicDistribution.toString();
                page.setPageTopicDistribution(topicDistribution);
                // Store it in the db
                db.savePageTopicDistribution(page);
            }

            // And the document topic dist.
            var documentTopicDistribution = ragService.getTextTopicDistribution(
                    DocumentTopicDistribution.class, document.getFullText());
            documentTopicDistribution.setDocument(document);
            document.setDocumentTopicDistribution(documentTopicDistribution);
            // Store it
            db.saveDocumentTopicDistribution(document);
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
     * @param jCas
     * @return
     */
    private List<Paragraph> getCoveredParagraphs(OCRPage page, JCas jCas) {
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
