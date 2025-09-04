package org.texttechnologylab.uce.common.models.topic;

import org.texttechnologylab.uce.common.annotations.Typesystem;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/***
 * TopicValueBaseWithScore class extends TopicValueBase class to represent a topic in a document with a score.
 * Therefore, if a topic have a label and a score, it can be represented by TopicValueBaseWithScore class.
 */
@Entity
@Table(name = "topicvaluebasewithscore")
@Typesystem(types = {org.texttechnologylab.annotation.TopicValueBaseWithScore.class})
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