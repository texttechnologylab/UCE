package org.texttechnologylab.models.emotion;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.*;

@Entity
@Table(name = "emotion_value")
public class EmotionValue extends ModelBase {

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "emotion_type_id", nullable = false)
    private EmotionType emotionType;

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    @Getter
    @Setter
    @Column(name = "value", nullable = false)
    private double value;

    public EmotionValue() {
    }

    public EmotionValue(EmotionType emotionType, Emotion emotion, double value) {
        this.emotionType = emotionType;
        this.emotion = emotion;
        this.value = value;
    }
}
