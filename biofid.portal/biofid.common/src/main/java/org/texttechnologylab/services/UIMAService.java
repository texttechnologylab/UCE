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

import java.io.File;

import java.io.FileInputStream;
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
    private final DatabaseService db;

    public UIMAService(GoetheUniversityService goetheUniversityService, DatabaseService db) {
        this.goetheUniversityService = goetheUniversityService;
        this.db = db;
    }

    /**
     * Imports all UIMA xmi files in a folder
     *
     * @param foldername
     * @return
     */
    public void XMIFolderDocumentsToDb(String foldername) {
        for (var file : Objects.requireNonNull(new File(foldername).listFiles())) {
            var doc = XMIToDocument(file.getPath());
            if (doc != null)
            {
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
    public Document XMIToDocument(String filename) {
        try {
            var jCas = JCasFactory.createJCas();
            // Read in the contents of a single xmi cas
            var file = new FileInputStream(filename);
            CasIOUtils.load(file, jCas.getCas());

            return XMIToDocument(jCas);
        } catch (IOException | UIMAException ex) {
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
    public Document XMIToDocument(JCas jCas) {

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
            var document = new Document(metadata.getLanguage(), metadata.getDocumentTitle(), metadata.getDocumentId());

            // Set the full text
            document.setFullText(jCas.getDocumentText());

            // See if we can get any more informatiom from the goethe collections
            document.setMetadataTitleInfo(goetheUniversityService.scrapeDocumentTitleInfo(document.getDocumentId()));

            // Set the cleaned full text. That is the sum of all tokens except of all anomalies
            var cleanedText = new StringJoiner(" ");
            JCasUtil.select(jCas, Token.class).forEach(t -> {
                // We dont want any tokens with suspicous chars here.
                if(t instanceof OCRToken ocr && ocr.getSuspiciousChars() > 0){
                    return;
                }
                var coveredAnomalies = JCasUtil.selectCovered(Anomaly.class, t).size();
                if(coveredAnomalies == 0) cleanedText.add(t.getCoveredText());
            });
            document.setFullTextCleaned(cleanedText.toString());

            // Set the sentences
            document.setSentences(JCasUtil.select(jCas, de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence.class)
                    .stream()
                    .map(s -> new org.texttechnologylab.models.corpus.Sentence(s.getBegin(), s.getEnd()))
                    .toList());

            // Set the named entities
            var nes = new ArrayList<org.texttechnologylab.models.corpus.NamedEntity>();
            JCasUtil.select(jCas, NamedEntity.class).forEach(ne -> {
                var xd = ne.getValue();
                // We don't want all NE types
                if (ne == null || ne.getValue() == null || !WANTED_NE_TYPES.contains(ne.getValue())) return;

                var namedEntity = new org.texttechnologylab.models.corpus.NamedEntity(ne.getBegin(), ne.getEnd());
                namedEntity.setType(ne.getValue());
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
