package org.texttechnologylab.uce.common.models.topic;

import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;

import javax.persistence.*;

@Entity
@Table(name = "topicword")
@Typesystem(types = {org.texttechnologylab.annotation.TopicWord.class})
public class TopicWord extends UIMAAnnotation {
    /***
     * TopicWord class can be used to represent a word in a topic with a probability value. Since each word belongs to
     * a topic, TopicWord class has a many-to-one relationship with TopicValueBase class.
     */

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private TopicValueBase topic;

    @Column(name = "word", nullable = false)
    private String word;

    @Column(name = "probability")
    private double probability;


    public TopicWord() {
        super(-1, -1);
    }

    public TopicWord(int begin, int end) {
        super(begin, end);
    }

    public TopicWord(int begin, int end, String coveredText) {
        super(begin, end);
        setCoveredText(coveredText);
    }

    public TopicValueBase getTopic() {
        return topic;
    }

    public void setTopic(TopicValueBase topic) {
        this.topic = topic;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }


}