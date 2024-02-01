package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.ModelBase;

import java.util.List;

/*
The documents should be scanned and extracted via OCR. This is a base class for that.
 */
public class Document extends ModelBase {

    private final String language;
    private final String documentTitle;
    private final String documentId;
    private String fullText;
    private List<Page> pages;
    private List<Sentence> sentences;
    private List<NamedEntity> namedEntities;
    private List<Time> times;
    private List<Taxon> taxons;
    private List<WikipediaLink> wikipediaLinks;

    public Document(String language, String documentTitle, String documentId){
        this.language = language;
        this.documentTitle = documentTitle;
        this.documentId = documentId;
    }

    public List<WikipediaLink> getWikipediaLinks() {
        return wikipediaLinks;
    }

    public void setWikipediaLinks(List<WikipediaLink> wikipediaLinks) {
        this.wikipediaLinks = wikipediaLinks;
    }

    public List<Taxon> getTaxons() {
        return taxons;
    }
    public void setTaxons(List<Taxon> taxons) {
        this.taxons = taxons;
    }

    public List<Time> getTimes() {
        return times;
    }
    public void setTimes(List<Time> times) {
        this.times = times;
    }

    public List<NamedEntity> getNamedEntities() {
        return namedEntities;
    }
    public void setNamedEntities(List<NamedEntity> namedEntities) {
        this.namedEntities = namedEntities;
    }

    public List<Sentence> getSentences() {
        return sentences;
    }
    public void setSentences(List<Sentence> sentences) {
        this.sentences = sentences;
    }

    public String getFullText() {
        return fullText;
    }
    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public List<Page> getPages() {
        return pages;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public String getLanguage() {
        return language;
    }
}
