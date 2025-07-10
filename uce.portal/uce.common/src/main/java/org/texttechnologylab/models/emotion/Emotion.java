package org.texttechnologylab.models.emotion;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "emotion")
@Typesystem(types = {org.texttechnologylab.annotation.Emotion.class})
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

    @Override
    public String getWikiId() {
        return "E" + "-" + this.getId();
    }

}
