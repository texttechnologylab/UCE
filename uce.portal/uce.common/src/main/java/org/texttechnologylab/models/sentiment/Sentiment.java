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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

    private SentimentValue getRepresentativeSentimentValue() {
        if (this.sentimentValues != null && !this.sentimentValues.isEmpty()) {
            return this.sentimentValues.stream()
                    .max(Comparator.comparingDouble(SentimentValue::getValue))
                    .orElse(null);
        }
        return null;
    }

    public String generateSentimentMarker() {
        SentimentValue rep = getRepresentativeSentimentValue();
        String repValue = rep != null ? rep.getSentimentType().getName() : "";
        return String.format("<span class='open-wiki-page annotation custom-context-menu sentiment-marker' title='%1$s' data-wid='%2$s' data-wcovered='%3$s' data-sentiment-value='%4$s'>s</span>", this.getWikiId(), this.getWikiId(), this.getCoveredText(), repValue);
    }

    public String generateSentimentCoveredStartSpan() {
        SentimentValue rep = getRepresentativeSentimentValue();
        String repValue = rep != null ? rep.getSentimentType().getName() : "";
        return String.format("<span class='sentiment-covered sentiment colorable-sentiment' id='s-%2$s' data-wcovered='%3$s' data-sentiment-value='%4$s'>", UUID.randomUUID(), this.getWikiId(), this.getCoveredText(), repValue);
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
