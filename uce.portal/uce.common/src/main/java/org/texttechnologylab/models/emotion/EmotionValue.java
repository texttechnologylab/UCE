package org.texttechnologylab.models.emotion;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;

@Entity
@Table(name = "emotion_value")
public class EmotionValue extends UIMAAnnotation implements WikiModel {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    public EmotionValue() {
        super(-1, -1);
    }

    public EmotionValue(int begin, int end) {
        super(begin, end);
    }

    public EmotionValue(int begin, int end, String coveredText) {
        super(begin, end);
        setCoveredText(coveredText);
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String getWikiId() {
        return "EV" + "-" + this.getId();
    }

}
