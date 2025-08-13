package org.texttechnologylab.models.sentiment;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.*;

@Entity
@Table(name = "sentiment_value")
public class SentimentValue extends ModelBase {
    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "sentiment_id", nullable = false)
    private Sentiment sentiment;
    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "sentiment_type_id", nullable = false)
    private SentimentType sentimentType;
    @Getter
    @Setter
    @Column(name = "value", nullable = false)
    private double value;

    public SentimentValue() {}

}
