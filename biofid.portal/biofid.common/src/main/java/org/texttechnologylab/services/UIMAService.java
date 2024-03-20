package org.texttechnologylab.services;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.Anomaly;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasIOUtils;
import org.springframework.stereotype.Service;
import org.texttechnologylab.annotation.ocr.*;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.gbif.GbifOccurrence;

import java.io.File;

import java.io.FileInputStream;
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
    private final DatabaseService db;
    private final GbifService gbifService;

    public UIMAService(GoetheUniversityService goetheUniversityService, DatabaseService db, GbifService gbifService) {
        this.goetheUniversityService = goetheUniversityService;
        this.db = db;
        this.gbifService = gbifService;
    }

    /**
     * Imports all UIMA xmi files in a folder
     *
     * @param foldername
     * @return
     */
    public void storeCorpusFromFolder(String foldername, String corpusName, String corpusAuthor) {
        var corpus = new Corpus();
        corpus.setName(corpusName);
        corpus.setAuthor(corpusAuthor);
        db.saveCorpus(corpus);

        for (var file : Objects.requireNonNull(new File(foldername).listFiles())) {
            var doc = XMIToDocument(file.getPath(), corpus.getId());
            if (doc != null) {
                db.saveDocument(doc);
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
    public Document XMIToDocument(String filename, long corpusId) {
        try {
            var jCas = JCasFactory.createJCas();
            // Read in the contents of a single xmi cas
            var file = new FileInputStream(filename);
            CasIOUtils.load(file, jCas.getCas());

            return XMIToDocument(jCas, corpusId);
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
    public Document XMIToDocument(JCas jCas, long corpusId) {

        // Read in the contents of a single xmi cas
        var unique = new HashSet<String>();
        JCasUtil.select(jCas, Annotation.class).stream().forEach(a -> {
            unique.add(a.getType().getName());
        });
        unique.forEach(a -> System.out.println(a));

        try {
            // First, metadata
            var metadata = JCasUtil.selectSingle(jCas, DocumentMetaData.class);
            if (metadata == null) {
                // If the metadata block is missing, something is off. In that case, return null
                return null;
            }
            var document = new Document(metadata.getLanguage(),
                    metadata.getDocumentTitle(),
                    metadata.getDocumentId(),
                    corpusId);

            // Set the full text
            document.setFullText(jCas.getDocumentText());

            // See if we can get any more informatiom from the goethe collections
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
            document.setSentences(JCasUtil.select(jCas, de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence.class)
                    .stream()
                    .map(s -> new org.texttechnologylab.models.corpus.Sentence(s.getBegin(), s.getEnd()))
                    .toList());

            // Set the named entities
            var nes = new ArrayList<org.texttechnologylab.models.corpus.NamedEntity>();
            JCasUtil.select(jCas, NamedEntity.class).forEach(ne -> {
                // We don't want all NE types
                if (ne == null || ne.getValue() == null) return;
                // We have different names for the types... sometimes they are full name, sometimes just the first three letters.
                var neType = "";
                for(var type:WANTED_NE_TYPES){
                    if(type.equals(ne.getValue()) || ne.getValue().equals(type.substring(0, 3))) neType = type;
                }
                if(neType.equals("")) return;

                var namedEntity = new org.texttechnologylab.models.corpus.NamedEntity(ne.getBegin(), ne.getEnd());
                namedEntity.setType(neType);
                namedEntity.setCoveredText(ne.getCoveredText());
                nes.add(namedEntity);
            });
            document.setNamedEntities(nes);

            // Set the times
            var times = new ArrayList<Time>();
            JCasUtil.select(jCas, org.texttechnologylab.annotation.type.Time.class).forEach(t -> {
                var time = new Time(t.getBegin(), t.getEnd());
                time.setValue(t.getValue());
                time.setCoveredText(t.getCoveredText());
                times.add(time);
            });
            document.setTimes(times);

            // Set the taxons
            var taxons = new ArrayList<Taxon>();
            JCasUtil.select(jCas, org.texttechnologylab.annotation.type.Taxon.class).forEach(t -> {
                var taxon = new Taxon(t.getBegin(), t.getEnd());
                taxon.setValue(t.getValue());
                taxon.setCoveredText(t.getCoveredText());
                taxon.setIdentifier(t.getIdentifier());
                // We need to handle taxons specifically, depending if they have annotated identifiers.
                if(taxon.getIdentifier() != null && !taxon.getIdentifier().isEmpty()){

                    // The recognized taxons should be split by a |
                    var occurrences = new ArrayList<GbifOccurrence>();
                    var splited = new ArrayList<String>();
                    // Sometimes they are delimitered by |, sometimes by space - who knows in this dump? :)
                    for(var split:taxon.getIdentifier().split("\\|")){
                        splited.addAll(Arrays.asList(split.split(" ")));
                    }

                    for(var potentialBiofidId: splited){
                        // The biofid urls are like: https://www.biofid.de/bio-ontologies/gbif/10428508
                        // We need the last number in that string, have a lookup into our sparsql database and from there fetch the
                        // correct TaxonId
                        if(potentialBiofidId.isEmpty()) continue;

                        var taxonId = gbifService.biofidIdUrlToGbifTaxonId(potentialBiofidId);
                        if(taxonId == -1) continue;
                        taxon.setGbifTaxonId(taxonId);

                        // Now check if we already have stored occurences for that taxon - we don't need to do that again then.
                        // We need to check in the current loop and in the database.
                        if(taxons.stream().anyMatch(ta -> ta.getGbifTaxonId() == taxonId)) break;
                        if(db.checkIfGbifOccurrencesExist(taxonId)) break;

                        // Otherwise, fetch new occurrences.
                        var potentialOccurrences = gbifService.scrapeGbifOccurrence(taxonId);
                        if(potentialOccurrences != null && !potentialOccurrences.isEmpty()){
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
            // Wikipedia/Wikidata?
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

            // Set the OCRpages
            var pages = new ArrayList<Page>();
            // We go through each page
            JCasUtil.select(jCas, OCRPage.class).forEach(p -> {
                // New page
                var page = new Page(p.getBegin(), p.getEnd(), p.getPageNumber(), p.getPageId());
                page.setParagraphs(getCoveredParagraphs(p, jCas));
                page.setBlocks(getCoveredBlocks(p, jCas));
                page.setLines(getCoveredLines(p, jCas));

                pages.add(page);
            });
            document.setPages(pages);

            return document;
        } catch (Exception ex) {
            return null;
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
