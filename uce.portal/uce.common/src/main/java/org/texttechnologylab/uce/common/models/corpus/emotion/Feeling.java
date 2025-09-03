package org.texttechnologylab.uce.common.models.corpus.emotion;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.annotation.AnnotationComment;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.ModelBase;

import javax.persistence.*;

/**
 * An Emotion annotation can have multiple feelings and these are stored as AnnotationComments in the XMI
 */
@Getter
@Setter
@Entity
@Table(name = "feeling")
@Typesystem(types = {AnnotationComment.class})
public class Feeling extends ModelBase {

    private String feeling;
    private double value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_id", nullable = true)
    private Emotion emotion;

    @Column(name = "emotion_id", insertable = false, updatable = false)
    private Long emotionId;

}
