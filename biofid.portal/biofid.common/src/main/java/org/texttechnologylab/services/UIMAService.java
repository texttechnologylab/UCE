package org.texttechnologylab.services;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.Anomaly;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticPredicate;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.texttechnologylab.annotation.semaf.semafsr.SrLink;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasIOUtils;
import org.springframework.stereotype.Service;
import org.texttechnologylab.annotation.ocr.*;
import org.texttechnologylab.config.CorpusConfig;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.gbif.GbifOccurrence;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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

        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            throw new MissingResourceException(
                    "The corpus folder did not contain a properly formatted corpusConfig.json", CorpusConfig.class.toString(), "");
        }

        //db.saveCorpus(corpus);

        for (var file : Objects.requireNonNull(
                new File(foldername)
                        .listFiles((dir, name) -> name.toLowerCase().endsWith(".xmi")))) {
            var doc = XMIToDocument(file.getPath(), corpus);
            if (doc != null) {
                db.saveDocument(doc);
                if(corpusConfig.getOther().isEnableEmbeddings())
                    postProccessDocuments(doc);
                System.out.println("Stored document with document id " + doc.getDocumentId());
            }
        }
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
            var file = new FileInputStream(filename);
            CasIOUtils.load(file, jCas.getCas());

            return XMIToDocument(jCas, corpus);
        } catch (Exception ex) {
            // TODO: Log!
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
        unique.forEach(a -> System.out.println(a));

        // JUST TESTING
        JCasUtil.select(jCas, SrLink.class).stream().forEach(a -> {
            var xd = "";
        });

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
                var pageSize = 3000;
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
            return null;
        }
    }

    /**
     * Here we apply any post processing of a document that isn't DUUI and needs the document to be stored once like
     * the rag vector embeddings
     */
    private void postProccessDocuments(Document document) {
        // Build the document embeddings for vector search and RAG
        var documentEmbeddings = ragService.getCompleteEmbeddingsFromDocument(document);
        // Store them
        for (var docEmbedding : documentEmbeddings) {
            ragService.saveDocumentEmbedding(docEmbedding);
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
