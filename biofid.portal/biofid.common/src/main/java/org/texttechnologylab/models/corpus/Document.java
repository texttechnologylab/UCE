package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.rag.DocumentEmbedding;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "document")
/*
The documents should be scanned and extracted via OCR. This is a base class for that.
 */
public class Document extends ModelBase {
    private String language;
    @Column(columnDefinition = "TEXT")
    private String documentTitle;
    private String documentId;
    private long corpusId;
    @Column(columnDefinition = "TEXT")
    private String fullText;

    @Column(columnDefinition = "TEXT")
    private String fullTextCleaned;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Page> pages;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Sentence> sentences;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<NamedEntity> namedEntities;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Time> times;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<Taxon> taxons;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_Id")
    private List<WikipediaLink> wikipediaLinks;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_id")
    private MetadataTitleInfo metadataTitleInfo;

    public Document() {
        metadataTitleInfo = new MetadataTitleInfo();
    }

    public Document(String language, String documentTitle, String documentId, long corpusId) {
        this.language = language;
        this.documentTitle = documentTitle;
        this.documentId = documentId;
        this.corpusId = corpusId;
    }

    public long getCorpusId() {
        return corpusId;
    }

    public void setCorpusId(long corpusId) {
        this.corpusId = corpusId;
    }

    public String getFullTextCleaned() {
        return fullTextCleaned;
    }

    public void setFullTextCleaned(String fullTextCleaned) {
        // Remove control characters: https://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
        fullTextCleaned = fullTextCleaned.replaceAll("\\p{Cntrl}", "");
        this.fullTextCleaned = fullTextCleaned;
    }

    public MetadataTitleInfo getMetadataTitleInfo() {
        return metadataTitleInfo;
    }

    public void setMetadataTitleInfo(MetadataTitleInfo metadataTitleInfo) {
        this.metadataTitleInfo = metadataTitleInfo;
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

    public String getFullTextSnippet(int take) {
        if (fullText == null || fullText.isEmpty()) {
            return "";
        }
        var words = Arrays.stream(fullText.trim().split("\\s+")).toList();
        // Take the first 30 words
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (String word : words.stream().skip(words.size() / 3).toList()) { // We skip the entry as its the table of content
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
     *
     * @return
     */
    public List<UIMAAnnotation> getAllAnnotations(int pagesSkip, int pagesTake) {
        var pagesBegin = getPages().stream().skip(pagesSkip).limit(1).findFirst().get().getBegin();
        var pagesEnd = getPages().stream().skip(Math.min(pagesSkip + pagesTake, getPages().size() - 1)).limit(1).findFirst().get().getEnd();

        var annotations = new ArrayList<UIMAAnnotation>();
        annotations.addAll(namedEntities.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(times.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(wikipediaLinks.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        annotations.addAll(taxons.stream().filter(a -> a.getBegin() >= pagesBegin && a.getEnd() <= pagesEnd).toList());
        return annotations;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText.replaceAll("<", "");
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public List<Page> getPages() {
        return pages;
    }

    public List<Page> getPages(int take, int skip) {
        return pages.stream()
                .sorted(Comparator.comparingInt(Page::getPageNumber))
                .skip(skip)
                .limit(take)
                .collect(Collectors.toList());
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentTitle() {
        var title = metadataTitleInfo.getTitle() == null ? documentTitle : metadataTitleInfo.getTitle();
        return title == null ? "(Unbekannt)" : title;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getLanguage() {
        return language;
    }
}
