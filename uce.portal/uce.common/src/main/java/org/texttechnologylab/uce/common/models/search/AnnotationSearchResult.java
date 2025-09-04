package org.texttechnologylab.uce.common.models.search;

public class AnnotationSearchResult {
    private String coveredText;
    private int documentId;
    private int occurrences;
    private String info;
    private long id;

    private int begin;
    private int end;
    private Long additionalId;
    private Long pageId;

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

    public AnnotationSearchResult(long id,
                                  String coveredText,
                                  int occurrences,
                                  String info,
                                  int documentId,
                                  Long additionalId,
                                  int begin,
                                  int end,
                                  Long pageId) {
        this.coveredText = coveredText;
        this.documentId = documentId;
        this.occurrences = occurrences;
        this.info = info;
        this.id = id;
        this.additionalId = additionalId;
        this.begin = begin;
        this.end = end;
        this.pageId = pageId;
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

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public Long getAdditionalId() {
        return additionalId;
    }

    public void setAdditionalId(Long additionalId) {
        this.additionalId = additionalId;
    }
}
