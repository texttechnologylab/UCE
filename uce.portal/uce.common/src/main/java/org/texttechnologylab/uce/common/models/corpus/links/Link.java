package org.texttechnologylab.uce.common.models.corpus.links;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * A link is a logical connection between (annotation) objects. There are several different levels of linking:
 * - Document to Document
 * - Annotation to Annotation
 * - Text to Text
 * And mixing all those. It's like a directed graph, and the link is an edge between two nodes.
 */
@MappedSuperclass
public class Link extends ModelBase {
    @Getter
    @Setter
    private long corpusId;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String type;

    @Getter
    @Setter
    private String linkId;

    /**
     * This is the PK of the document or annotation in the db, so the 'id'.
     */
    @Getter
    @Setter
    private long fromId;

    /**
     * This is the PK of the document or annotation in the db, so the 'id'.
     */
    @Getter
    @Setter
    private long toId;

    /*
    Interesting info concerning supertypes. We don't know what kind of annotation this link points to.
    We could use Hibernates @Inheritance and @Discriminator logic, but that leaves things to Hibernate
    and in the end, if you'd rename the table of e.g. namedentity, you will still need a manual migration
    to all foreign keys and what not... I don't see the benefit over simply storing the table name  of the annotation as well.
    Makes things much easier and from what I read, the outcome is pretty similar.
     */

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String fromAnnotationTypeTable;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String toAnnotationTypeTable;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String fromAnnotationType;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String toAnnotationType;
}
