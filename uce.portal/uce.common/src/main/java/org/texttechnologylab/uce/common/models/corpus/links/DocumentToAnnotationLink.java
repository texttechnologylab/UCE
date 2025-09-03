package org.texttechnologylab.uce.common.models.corpus.links;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "documenttoannotationlink")
public class DocumentToAnnotationLink extends Link{
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
}
