package org.texttechnologylab.models.emotion;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.modelInfo.NamedModel;
import org.texttechnologylab.utils.Pair;

import javax.persistence.*;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "emotion")
@Typesystem(types = {org.texttechnologylab.annotation.Emotion.class})
@NamedModel(name = "emotion")
public class Emotion extends UIMAAnnotation implements WikiModel {

    @Getter
    @Setter
    @OneToMany(mappedBy = "emotion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<EmotionValue> emotionValues;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    public Emotion() {
        super(-1, -1);
    }

    public Emotion(int begin, int end) {
        super(begin, end);
    }

    public Emotion(int begin, int end, String coveredText) {
        super(begin, end);
        setCoveredText(coveredText);
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    private EmotionValue getRepresentativeEmotionValue() {
        if (this.emotionValues != null && !this.emotionValues.isEmpty()) {
            return this.emotionValues.stream().max(Comparator.comparingDouble(EmotionValue::getValue)).orElse(null);
        }
        return null;
    }

    public String generateEmotionMarker() {
        String repEmotionValue = "";
        if (this.getEmotionValues() != null && !this.getEmotionValues().isEmpty()) {
            var repEmotion = this.getRepresentativeEmotionValue();
            if (repEmotion != null) {
                repEmotionValue = repEmotion.getEmotionType().getName();
            }
        }
        return String.format("<span class='open-wiki-page annotation custom-context-menu emotion-marker' title='%1$s' data-wid='%2$s' data-wcovered='%3$s' data-emotion-value='%4$s'>e</span>", this.getWikiId(), this.getWikiId(), this.getCoveredText(), repEmotionValue);
    }

    public String generateEmotionCoveredStartSpan() {
        String repEmotionValue = "";
        if (this.getEmotionValues() != null && !this.getEmotionValues().isEmpty()) {
            var repEmotion = this.getRepresentativeEmotionValue();
            if (repEmotion != null) {
                repEmotionValue = repEmotion.getEmotionType().getName();
            }
        }
        return String.format("<span class='emotion-covered emotion colorable-emotion' id='emot-%2$s' data-wcovered='%3$s' data-emotion-value='%4$s'>", UUID.randomUUID(), this.getWikiId(), this.getCoveredText(), repEmotionValue);
    }

    public List<Pair<String, Double>> collectEmotionValues() {
        return this.emotionValues.stream().map(ev -> new Pair<>(ev.getEmotionType().getName(), ev.getValue())).toList();
    }

    @Override
    public String getWikiId() {
        return "E" + "-" + this.getId();
    }

}
