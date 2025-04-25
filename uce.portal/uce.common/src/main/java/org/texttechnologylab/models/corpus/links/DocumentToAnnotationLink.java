package org.texttechnologylab.models.corpus.links;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "documenttoannotationlink")
public class DocumentToAnnotationLink extends Link{
    /*
    Interesting info concerning supertypes. We don't know what kind of annotation this link points to.
    We could use Hibernates @Inheritance and @Discriminator logic, but that leaves things to Hibernate
    and in the end, if you'd rename the table of e.g. namedentity, you will still need a manual migration
    to all foreign keys and what not... I don't see the benefit over simply storing the table name  of the annotation as well.
    Makes things much easier and from what I read, the outcome is pretty similar.
     */
    @Column(columnDefinition = "TEXT")
    private String toAnnotationTypeTable;
    @Column(columnDefinition = "TEXT")
    private String toAnnotationType;
    @Column(name = "\"fromm\"", columnDefinition = "TEXT")
    private String from;
    @Column(name = "\"too\"", columnDefinition = "TEXT")
    private String to;
    @Column(name = "\"tocoveredtext\"", columnDefinition = "TEXT")
    private String toCoveredText;
    /**
     * The begin index of the annotation this link points to.
     */
    private int toBegin;
    /**
     * The end index of the annotation this link points to.
     */
    private int toEnd;

    public DocumentToAnnotationLink(){}

    public String getToAnnotationTypeTable() {
        return toAnnotationTypeTable;
    }

    public void setToAnnotationTypeTable(String toAnnotationTypeTable) {
        this.toAnnotationTypeTable = toAnnotationTypeTable;
    }

    public String getToAnnotationType() {
        return toAnnotationType;
    }

    public void setToAnnotationType(String toAnnotationType) {
        this.toAnnotationType = toAnnotationType;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public int getToBegin() {
        return toBegin;
    }

    public void setToBegin(int toBegin) {
        this.toBegin = toBegin;
    }

    public int getToEnd() {
        return toEnd;
    }

    public void setToEnd(int toEnd) {
        this.toEnd = toEnd;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getToCoveredText() {
        return toCoveredText;
    }

    public void setToCoveredText(String to) {
        this.toCoveredText = to;
    }
}
