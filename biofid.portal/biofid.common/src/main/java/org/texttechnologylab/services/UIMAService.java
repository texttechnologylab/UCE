package org.texttechnologylab.services;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Location;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.unihd.dbs.uima.types.heideltime.Sentence_Type;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.springframework.stereotype.Service;
import org.texttechnologylab.annotation.ocr.OCRBlock;
import org.texttechnologylab.annotation.ocr.OCRLine;
import org.texttechnologylab.annotation.ocr.OCRPage;
import org.texttechnologylab.annotation.ocr.OCRParagraph;
import org.texttechnologylab.models.corpus.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Service
/*
TODO: Add logging!
Holds logic for interacting with any UIMA associated data
 */
public class UIMAService {

    private static final Set<String> WANTED_NE_TYPES = Set.of(
            "LOC", "MISC", "PER", "ORG"
    );

    public UIMAService() {
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
            var file = new FileInputStream(new File(filename));
            CasIOUtils.load(file, jCas.getCas());

            return XMIToDocument(jCas);
        } catch (Exception ex) {
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
            // Set the sentences
            document.setSentences(JCasUtil.select(jCas, de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence.class)
                    .stream()
                    .map(s -> new org.texttechnologylab.models.corpus.Sentence(s.getBegin(), s.getEnd()))
                    .toList());

            // Set the named entities
            var nes = new ArrayList<org.texttechnologylab.models.corpus.NamedEntity>();
            JCasUtil.select(jCas, NamedEntity.class).forEach(ne -> {
                // We don't want all NE types
                if (ne == null || ne.getValue() == null || !WANTED_NE_TYPES.contains(ne.getValue())) return;
                var namedEntity = new org.texttechnologylab.models.corpus.NamedEntity(ne.getBegin(), ne.getEnd());
                namedEntity.setType(ne.getValue());
                namedEntity.setLemmaValue(String.join(" ", JCasUtil.selectCovered(Lemma.class, ne)
                        .stream()
                        .map(Lemma::getValue)
                        .toArray(String[]::new)));
                nes.add(namedEntity);
            });
            document.setNamedEntities(nes);

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

            paragraphs.add(paragraph);
        });
        return paragraphs;
    }

}
