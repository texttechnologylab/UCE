package org.texttechnologylab.models.sentiment;

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
import java.util.List;

@Entity
@Table(name = "sentiment")
@Typesystem(types = {org.texttechnologylab.annotation.SentimentModel.class})
@NamedModel(name = "sentiment")
public class Sentiment extends UIMAAnnotation implements WikiModel {

    @Getter
    @Setter
    @Column(name = "sentiment", nullable = false)
    private int sentiment;

    @Getter
    @Setter
    @OneToMany(mappedBy = "sentiment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<SentimentValue> sentimentValues;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    public Sentiment() {
        super(-1, -1);
    }

    public Sentiment(int begin, int end) {
        super(begin, end);
    }

    public Sentiment(int begin, int end, String coveredText) {
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
        return "S" + "-" + this.getId();
    }

    public List<Pair<String, String>> loopThroughProperties() {
        return sentimentValues.stream()
                .map(v -> new Pair<>(v.getSentimentType().getName(), String.format("%.6f", v.getValue())))
                .toList();
    }
}
