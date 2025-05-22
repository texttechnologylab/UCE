package org.texttechnologylab.config.corpusConfig;

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
    private boolean geoNames;
    private boolean sentence;
    private boolean time;
    private boolean wikipediaLink;
    // negation annos
    private boolean completeNegation;
    private boolean cue;
    private boolean event;
    private boolean focus;
    private boolean scope;
    private boolean xscope;
    private boolean unifiedTopic;

    public boolean isGeoNames() {
        return geoNames;
    }

    public void setGeoNames(boolean geoNames) {
        this.geoNames = geoNames;
    }

    public void setUnifiedTopic(boolean unifiedTopic) {
        this.unifiedTopic = unifiedTopic;
    }

    public boolean isLogicalLinks() {
        return logicalLinks;
    }

    public void setLogicalLinks(boolean logicalLinks) {
        this.logicalLinks = logicalLinks;
    }

    public boolean isUceMetadata() {
        return uceMetadata;
    }

    public void setUceMetadata(boolean uceMetadata) {
        this.uceMetadata = uceMetadata;
    }

    public boolean isAnnotatorMetadata() {
        return annotatorMetadata;
    }

    public void setAnnotatorMetadata(boolean annotatorMetadata) {
        this.annotatorMetadata = annotatorMetadata;
    }

    public boolean isLemma() {
        return lemma;
    }

    public void setLemma(boolean lemma) {
        this.lemma = lemma;
    }

    public boolean isSrLink() {
        return srLink;
    }

    public void setSrLink(boolean srLink) {
        this.srLink = srLink;
    }

    public boolean isOCRPage() {
        return OCRPage;
    }

    public void setOCRPage(boolean OCRPage) {
        this.OCRPage = OCRPage;
    }

    public boolean isOCRParagraph() {
        return OCRParagraph;
    }

    public void setOCRParagraph(boolean OCRParagraph) {
        this.OCRParagraph = OCRParagraph;
    }

    public boolean isOCRBlock() {
        return OCRBlock;
    }

    public void setOCRBlock(boolean OCRBlock) {
        this.OCRBlock = OCRBlock;
    }

    public boolean isOCRLine() {
        return OCRLine;
    }

    public void setOCRLine(boolean OCRLine) {
        this.OCRLine = OCRLine;
    }

    public TaxonConfig getTaxon() {
        return taxon;
    }

    public void setTaxon(TaxonConfig taxon) {
        this.taxon = taxon;
    }

    public boolean isNamedEntity() {
        return namedEntity;
    }

    public void setNamedEntity(boolean namedEntity) {
        this.namedEntity = namedEntity;
    }

    public boolean isSentence() {
        return sentence;
    }

    public void setSentence(boolean sentence) {
        this.sentence = sentence;
    }

    public boolean isTime() {
        return time;
    }

    public void setTime(boolean time) {
        this.time = time;
    }

    public boolean isWikipediaLink() {
        return wikipediaLink;
    }

    public void setWikipediaLink(boolean wikipediaLink) {
        this.wikipediaLink = wikipediaLink;
    }

    // negations
    public boolean isCompleteNegation() {
        return completeNegation;
    }

    public void setCompleteNegation(boolean completeNegation) {
        this.completeNegation = completeNegation;
    }

    public boolean isCue() {
        return cue;
    }

    public void setCue(boolean cue) {
        this.cue = cue;
    }

    public boolean isEvent() {
        return event;
    }

    public void setEvent(boolean event) {
        this.event = event;
    }

    public boolean isFocus() {
        return focus;
    }

    public void setFocus(boolean focus) {
        this.focus = focus;
    }

    public boolean isScope() {
        return scope;
    }

    public void setScope(boolean scope) {
        this.scope = scope;
    }

    public boolean isXscope() {
        return xscope;
    }

    public void setXscope(boolean xscope) {
        this.xscope = xscope;
    }

    // UnifiedTopic
    public boolean isUnifiedTopic() {
        return unifiedTopic;
    }

}
