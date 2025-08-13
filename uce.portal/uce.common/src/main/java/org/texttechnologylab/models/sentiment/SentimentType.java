package org.texttechnologylab.models.sentiment;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sentiment_type")
public class SentimentType extends ModelBase {
    @Getter
    @Setter
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public SentimentType() {
    }
    public SentimentType(String name) {
        this.name = name;
    }

}
