package org.texttechnologylab.uce.common.models.corpus.emotion;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.models.corpus.Sentence;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "sentenceemotion")
@IdClass(SentenceEmotion.SentenceEmotionId.class)
public class SentenceEmotion {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sentence_id", nullable = false)
    private Sentence sentence;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    @Id
    @Column(name = "model", nullable = false, length = 255)
    private String model;

    @Id
    @Column(name = "feeling", nullable = false, length = 255)
    private String feeling;

    @Column(name = "value")
    private Double value;

    public SentenceEmotion() {}

    public SentenceEmotion(Sentence sentence, Emotion emotion, String model, String feeling, Double value) {
        this.sentence = sentence;
        this.emotion = emotion;
        this.model = model;
        this.feeling = feeling;
        this.value = value;
    }

    public static class SentenceEmotionId implements Serializable {
        private Long sentence; // references Sentence.id
        private Long emotion;  // references Emotion.id
        private String model;
        private String feeling;

        public SentenceEmotionId() {}

        public SentenceEmotionId(Long sentence, Long emotion, String model, String feeling) {
            this.sentence = sentence;
            this.emotion = emotion;
            this.model = model;
            this.feeling = feeling;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SentenceEmotionId that)) return false;
            return Objects.equals(sentence, that.sentence)
                    && Objects.equals(emotion, that.emotion)
                    && Objects.equals(model, that.model)
                    && Objects.equals(feeling, that.feeling);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sentence, emotion, model, feeling);
        }
    }
}
