package org.texttechnologylab.uce.common.config.corpusConfig;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CorpusAnnotationConfig {
    private boolean annotatorMetadata;
    private boolean uceMetadata;
    private boolean logicalLinks;
    private boolean OCRPage;
    private boolean OCRParagraph;
    private boolean OCRBlock;
    private boolean OCRLine;
    private TaxonConfig taxon;
    private boolean srLink;
    private boolean lemma;
    private boolean namedEntity;
    private boolean sentiment;
    private boolean emotion;
    private boolean geoNames;
    private boolean sentence;
    private boolean time;
    private boolean wikipediaLink;
    private boolean image;
    // negation annos
    private boolean completeNegation;
    private boolean cue;
    private boolean event;
    private boolean focus;
    private boolean scope;
    private boolean xscope;
    private boolean unifiedTopic;
}
