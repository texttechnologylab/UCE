package org.texttechnologylab.models.topic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "topicvaluebasewithscore")
public class TopicValueBaseWithScore extends TopicValueBase {
    /***
     * TopicValueBaseWithScore class extends TopicValueBase class to represent a topic in a document with a score.
     * Therefore, if a topic have a label and a score, it can be represented by TopicValueBaseWithScore class.
     */

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