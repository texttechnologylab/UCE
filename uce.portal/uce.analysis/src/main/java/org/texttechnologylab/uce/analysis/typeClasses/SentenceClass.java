package org.texttechnologylab.uce.analysis.typeClasses;

import java.util.ArrayList;

public class SentenceClass {

    private String text;

    private String language;

    private int begin;

    private int end;

    private ArrayList<TopicClass> allTopics = new ArrayList<>();

    private ArrayList<OffensiveClass> allOffensives = new ArrayList<>();

    private ArrayList<HateClass> allHates = new ArrayList<>();

    private ArrayList<SentimentClass> allSentiments = new ArrayList<>();

    private ArrayList<ToxicClass> allToxics = new ArrayList<>();

    private ArrayList<EmotionClass> allEmotions = new ArrayList<>();

    private ArrayList<FactClass> allFacts = new ArrayList<>();

    private ArrayList<CoherenceClass> allCoherences = new ArrayList<>();

    private ArrayList<StanceClass> allStances = new ArrayList<>();

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

    public void addOffensive(OffensiveClass offensive) {
        this.allOffensives.add(offensive);
    }

    public void addStance(StanceClass stance) {
        this.allStances.add(stance);
    }
    public ArrayList<StanceClass> getAllStances() {
        return allStances;
    }
    public void setAllStances(ArrayList<StanceClass> allStances) {
        this.allStances = allStances;
    }

    public ArrayList<TopicClass> getAllTopics() {
        return allTopics;
    }

    public void setAllTopics(ArrayList<TopicClass> allTopics) {
        this.allTopics = allTopics;
    }
    public ArrayList<OffensiveClass> getAllOffensives() {
        return allOffensives;
    }

    public void setAllOffensives(ArrayList<OffensiveClass> allOffensives) {
        this.allOffensives = allOffensives;
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

    public void addToxic(ToxicClass toxic) {
        this.allToxics.add(toxic);
    }
    public ArrayList<ToxicClass> getAllToxics() {
        return allToxics;
    }
    public void setAllSentiments(ArrayList<SentimentClass> allSentiments) {
        this.allSentiments = allSentiments;
    }
    public void setAllToxics(ArrayList<ToxicClass> allToxics) {
        this.allToxics = allToxics;
    }

    public void addEmotion(EmotionClass emotion) {
        this.allEmotions.add(emotion);
    }
    public ArrayList<EmotionClass> getAllEmotions() {
        return allEmotions;
    }
    public void setAllEmotions(ArrayList<EmotionClass> allEmotions) {
        this.allEmotions = allEmotions;
    }

    public void addFact(FactClass fact) {
        this.allFacts.add(fact);
    }

    public ArrayList<FactClass> getAllFacts() {
        return allFacts;
    }

    public void addCoherence(CoherenceClass coherence) {
        this.allCoherences.add(coherence);
    }
    public ArrayList<CoherenceClass> getAllCoherences() {
        return allCoherences;
    }


}
