package org.texttechnologylab.uce.common.models.corpus.links;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "annotationtodocumentlink")
public class AnnotationToDocumentLink extends Link{

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
}
