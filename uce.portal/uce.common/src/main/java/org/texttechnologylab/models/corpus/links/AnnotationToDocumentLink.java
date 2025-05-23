package org.texttechnologylab.models.corpus.links;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "annotationtodocumentlink")
public class AnnotationToDocumentLink extends Link{

    @Column(columnDefinition = "TEXT")
    private String fromAnnotationTypeTable;

    @Column(columnDefinition = "TEXT")
    private String fromAnnotationType;

    @Column(name = "\"fromm\"", columnDefinition = "TEXT")
    private String from;

    @Column(name = "\"too\"", columnDefinition = "TEXT")
    private String to;

    @Column(name = "\"fromcoveredtext\"", columnDefinition = "TEXT")
    private String fromCoveredText;

    /**
     * The begin index of the annotation this link points from.
     */
    private int fromBegin;
    /**
     * The end index of the annotation this link points from.
     */
    private int fromEnd;

    public AnnotationToDocumentLink(){}

    public String getFromAnnotationTypeTable() {
        return fromAnnotationTypeTable;
    }

    public void setFromAnnotationTypeTable(String fromAnnotationTypeTable) {
        this.fromAnnotationTypeTable = fromAnnotationTypeTable;
    }

    public String getFromAnnotationType() {
        return fromAnnotationType;
    }

    public void setFromAnnotationType(String fromAnnotationType) {
        this.fromAnnotationType = fromAnnotationType;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFromCoveredText() {
        return fromCoveredText;
    }

    public void setFromCoveredText(String fromCoveredText) {
        this.fromCoveredText = fromCoveredText;
    }

    public int getFromBegin() {
        return fromBegin;
    }

    public void setFromBegin(int fromBegin) {
        this.fromBegin = fromBegin;
    }

    public int getFromEnd() {
        return fromEnd;
    }

    public void setFromEnd(int fromEnd) {
        this.fromEnd = fromEnd;
    }
}
