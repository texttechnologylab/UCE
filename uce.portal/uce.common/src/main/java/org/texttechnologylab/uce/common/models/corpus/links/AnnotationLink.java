package org.texttechnologylab.uce.common.models.corpus.links;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "annotationlink")
public class AnnotationLink extends Link{

    @Getter
    @Setter
    @Column(name = "\"fromm\"", columnDefinition = "TEXT")
    private String from;

    @Getter
    @Setter
    @Column(name = "\"too\"", columnDefinition = "TEXT")
    private String to;

    @Getter
    @Setter
    @Column(name = "\"fromcoveredtext\"", columnDefinition = "TEXT")
    private String fromCoveredText;

    @Getter
    @Setter
    @Column(name = "\"tocoveredtext\"", columnDefinition = "TEXT")
    private String toCoveredText;

    /**
     * The begin index of the annotation this link points from.
     */
    @Getter
    @Setter
    private int fromBegin;

    /**
     * The begin index of the annotation this link points to.
     */
    @Getter
    @Setter
    private int toBegin;

    /**
     * The end index of the annotation this link points from.
     */
    @Getter
    @Setter
    private int fromEnd;

    /**
     * The end index of the annotation this link points to.
     */
    @Getter
    @Setter
    private int toEnd;

    public AnnotationLink(){}

}
