package org.texttechnologylab.config.corpusConfig;

public class CorpusAnnotationConfig{
    private boolean OCRPage;
    private boolean OCRParagraph;
    private boolean OCRBlock;
    private boolean OCRLine;
    private TaxonConfig taxon;
    private boolean srLink;
    private boolean namedEntity;
    private boolean sentence;
    private boolean time;
    private boolean wikipediaLink;

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
}
