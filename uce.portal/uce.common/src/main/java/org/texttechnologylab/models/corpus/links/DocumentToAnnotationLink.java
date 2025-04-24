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
    We could use Hibernates @Inheritance and @Distcriminator logic, but that leaves things to Hibernate
    and in the end, if you'd rename the table of e.g. namedentity, you will still need a manual migration
    to all foreign keys and what not... I don't see the benefit over simply storing the table name  of the annotation as well.
    Makes things much easier and from what I read, the outcome is pretty similar.
     */
    @Column(columnDefinition = "TEXT")
    private String toAnnotationType;

}
