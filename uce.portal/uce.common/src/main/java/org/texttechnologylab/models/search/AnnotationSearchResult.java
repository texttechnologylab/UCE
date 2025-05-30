package org.texttechnologylab.models.search;

import org.texttechnologylab.models.WikiModel;

public class AnnotationSearchResult {
    private String coveredText;
    private int documentId;
    private int occurrences;
    private String info;
    private long id;

    public AnnotationSearchResult(long id,
                                  String coveredText,
                                  int occurrences,
                                  String info,
                                  int documentId) {
        this.coveredText = coveredText;
        this.documentId = documentId;
        this.occurrences = occurrences;
        this.info = info;
        this.id = id;
    }

    public AnnotationSearchResult(){}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCoveredText() {
        return coveredText;
    }

    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
    }

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
