package org.texttechnologylab.models.sentiment;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;

@Entity
@Table(name = "sentiment")
public class Sentiment extends UIMAAnnotation implements WikiModel {

    @Getter
    @Setter
    @Column(name = "sentiment", nullable = false)
    private int sentiment;

    @Getter
    @Setter
    @Column(name = "probability_positive", nullable = false)
    private double probabilityPositive;

    @Getter
    @Setter
    @Column(name = "probability_neutral", nullable = false)
    private double probabilityNeutral;

    @Getter
    @Setter
    @Column(name = "probability_negative", nullable = false)
    private double probabilityNegative;


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
}
