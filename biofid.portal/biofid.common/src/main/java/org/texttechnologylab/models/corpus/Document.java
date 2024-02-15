package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.UIMAAnnotation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
The documents should be scanned and extracted via OCR. This is a base class for that.
 */
public class Document extends ModelBase {

    private final String language;
    private String documentTitle;
    private final String documentId;
    private String fullText;
    private String fullTextCleaned;
    private List<Page> pages;
    private List<Sentence> sentences;
    private List<NamedEntity> namedEntities;
    private List<Time> times;
    private List<Taxon> taxons;
    private List<WikipediaLink> wikipediaLinks;
    private GoetheTitleInfo goetheTitleInfo;

    public Document(String language, String documentTitle, String documentId) {
        this.language = language;
        this.documentTitle = documentTitle;
        this.documentId = documentId;
    }

    public String getFullTextCleaned() {
        return fullTextCleaned;
    }

    public void setFullTextCleaned(String fullTextCleaned) {
        // Remove control characters: https://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
        fullTextCleaned = fullTextCleaned.replaceAll("\\p{Cntrl}", "");
        this.fullTextCleaned = fullTextCleaned;
    }

    public GoetheTitleInfo getGoetheTitleInfo() {
        return goetheTitleInfo;
    }

    public void setGoetheTitleInfo(GoetheTitleInfo goetheTitleInfo) {
        this.goetheTitleInfo = goetheTitleInfo;
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

    public String getFullTextCleanedSnippet(int take) {
        if (fullTextCleaned == null || fullTextCleaned.isEmpty()) {
            return "";
        }
        String[] words = fullTextCleaned.trim().split("\\s+");
        // Take the first 30 words
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (String word : words) {
            result.append(word).append(" ");
            count++;
            if (count == take) {
                break;
            }
        }
        return result.toString().trim();
    }

    /**
     * Gets all objects of type UIMAAnnotation of this document
     * @return
     */
    public List<UIMAAnnotation> getAllAnnotations(){
        var annotations = new ArrayList<UIMAAnnotation>();
        annotations.addAll(namedEntities);
        annotations.addAll(times);
        annotations.addAll(wikipediaLinks);
        annotations.addAll(taxons);
        return annotations;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public List<Page> getPages(int take, int skip) {
        return pages.stream()
                .sorted(Comparator.comparingInt(Page::getPageNumber))
                .skip(skip)
                .limit(skip + take)
                .collect(Collectors.toList());
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentTitle() {
        var title = goetheTitleInfo.getTitle() == null ? documentTitle : goetheTitleInfo.getTitle();
        return title == null ? "(-)" : title;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getLanguage() {
        return language;
    }
}
