package org.texttechnologylab;

import java.util.ArrayList;

public class SentenceClass {

    private String text;

    private String language;

    private int begin;

    private int end;

    private ArrayList<TopicClass> allTopics = new ArrayList<>();

    private ArrayList<HateClass> allHates = new ArrayList<>();

    private ArrayList<SentimentClass> allSentiments = new ArrayList<>();

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }
    public void setEnd(int end) {
        this.end = end;
    }

    public void addTopic(TopicClass topic) {
        this.allTopics.add(topic);
    }

    public ArrayList<TopicClass> getAllTopics() {
        return allTopics;
    }

    public void setAllTopics(ArrayList<TopicClass> allTopics) {
        this.allTopics = allTopics;
    }
    public void addHate(HateClass hate) {
        this.allHates.add(hate);
    }
    public ArrayList<HateClass> getAllHates() {
        return allHates;
    }

    public void setAllHates(ArrayList<HateClass> allHates) {
        this.allHates = allHates;
    }

    public void addSentiment(SentimentClass sentiment) {
        this.allSentiments.add(sentiment);
    }

    public ArrayList<SentimentClass> getAllSentiments() {
        return allSentiments;
    }
}
