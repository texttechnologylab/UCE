package org.texttechnologylab.models.topic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "topicvaluebasewithscore")
public class TopicValueBaseWithScore extends TopicValueBase {

    @Column(name = "score", nullable = false)
    private double score;


    public TopicValueBaseWithScore() {
        super();
    }

    public TopicValueBaseWithScore(int begin, int end) {
        super(begin, end);
    }

    public TopicValueBaseWithScore(int begin, int end, String coveredText) {
        super(begin, end, coveredText);
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

}