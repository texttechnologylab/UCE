package org.texttechnologylab.uce.common.models.corpus.emotion;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.ModelEntity;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.Sentence;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sentenceemotions")
public class SentenceEmotions extends ModelBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id", nullable = false)
    private Sentence sentence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelEntity model;

    public SentenceEmotions(Sentence sentence, Emotion emotion, ModelEntity model) {
        this.sentence = sentence;
        this.emotion = emotion;
        this.model = model;
    }
}
