package org.texttechnologylab;

import java.util.ArrayList;
import java.util.HashMap;

public class TextClass {

    private HashMap<ModelInfo, ArrayList<TopicClass>> topics = new HashMap<>();
    private HashMap<ModelInfo, ArrayList<HateClass>> hate = new HashMap<>();
    private HashMap<ModelInfo, ArrayList<SentimentClass>> sentiment = new HashMap<>();

    private HashMap<ModelInfo, HateClass> hateAVG = new HashMap<>();

    private HashMap<ModelInfo, SentimentClass> sentimentAVG = new HashMap<>();

    private HashMap<ModelInfo, TopicClass> topicAVG = new HashMap<>();

    public void addTopic(ModelInfo model, TopicClass topic) {
        if (!this.topics.containsKey(model)) {
            this.topics.put(model, new ArrayList<>());
        }
        this.topics.get(model).add(topic);
    }
    public ArrayList<TopicClass> getTopic(ModelInfo model) {
        if (!this.topics.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.topics.get(model);
    }
    public void deleteTopic(ModelInfo model) {
        this.topics.remove(model);
    }

    public void addHate(ModelInfo model, HateClass hate) {
        if (!this.hate.containsKey(model)) {
            this.hate.put(model, new ArrayList<>());
        }
        this.hate.get(model).add(hate);
    }

    public void computeAVGHate(){
        for (ModelInfo model : this.hate.keySet()) {
            ArrayList<HateClass> hateList = this.hate.get(model);
            double totalHate = 0;
            double totalNonHate = 0;
            for (HateClass hate : hateList) {
                totalHate += hate.getHate();
                totalNonHate += hate.getNonHate();
            }
            HateClass avgHate = new HateClass();
            avgHate.setHate(totalHate / hateList.size());
            avgHate.setNonHate(totalNonHate / hateList.size());
            this.hateAVG.put(model, avgHate);
        }
    }

    public void computeAVGSentiment(){
        for (ModelInfo model : this.sentiment.keySet()) {
            ArrayList<SentimentClass> sentimentList = this.sentiment.get(model);
            double totalNegative = 0;
            double totalPositive = 0;
            double totalNeutral = 0;
            for (SentimentClass sentiment : sentimentList) {
                totalNegative += sentiment.getNegative();
                totalPositive += sentiment.getPositive();
                totalNeutral += sentiment.getNeutral();
            }
            SentimentClass avgSentiment = new SentimentClass();
            avgSentiment.setNegative(totalNegative / sentimentList.size());
            avgSentiment.setPositive(totalPositive / sentimentList.size());
            avgSentiment.setNeutral(totalNeutral / sentimentList.size());
            this.sentimentAVG.put(model, avgSentiment);
        }
    }

    public void computeAVGTopic(){
        for (ModelInfo model : this.topics.keySet()) {
            HashMap<String, Double> topicScores = new HashMap<>();
            ArrayList<TopicClass> topicList = this.topics.get(model);
            for (TopicClass topic : topicList) {
                ArrayList<TopicInput> topics = topic.getTopics();
                for (TopicInput topicInput : topics) {
                    String key = topicInput.getKey();
                    double score = topicInput.getScore();
                    topicScores.put(key, topicScores.getOrDefault(key, 0.0) + score);
                }
            }
            TopicClass avgTopic = new TopicClass();
            ArrayList<TopicInput> avgTopics = new ArrayList<>();
            for (String key : topicScores.keySet()) {
                TopicInput topicInput = new TopicInput();
                topicInput.setKey(key);
                topicInput.setScore(topicScores.get(key) / topicList.size());
                avgTopics.add(topicInput);
            }
            avgTopic.setTopics(avgTopics);
            this.topicAVG.put(model, avgTopic);
        }
    }

    public HashMap<ModelInfo, HateClass> getHateAVG() {
        return hateAVG;
    }

    public HashMap<ModelInfo, SentimentClass> getSentimentAVG() {
        return sentimentAVG;
    }

    public HashMap<ModelInfo, TopicClass> getTopicAVG() {
        return topicAVG;
    }

    public HateClass getHateAVG(ModelInfo model) {
        if (!this.hateAVG.containsKey(model)) {
            return new HateClass();
        }
        return this.hateAVG.get(model);
    }

    public SentimentClass getSentimentAVG(ModelInfo model) {
        if (!this.sentimentAVG.containsKey(model)) {
            return new SentimentClass();
        }
        return this.sentimentAVG.get(model);
    }

    public TopicClass getTopicAVG(ModelInfo model) {
        if (!this.topicAVG.containsKey(model)) {
            return new TopicClass();
        }
        return this.topicAVG.get(model);
    }



    public ArrayList<HateClass> getHate(ModelInfo model) {
        if (!this.hate.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.hate.get(model);
    }

    public void deleteHate(ModelInfo model) {
        this.hate.remove(model);
    }
    public void addSentiment(ModelInfo model, SentimentClass sentiment) {
        if (!this.sentiment.containsKey(model)) {
            this.sentiment.put(model, new ArrayList<>());
        }
        this.sentiment.get(model).add(sentiment);
    }
    public ArrayList<SentimentClass> getSentiment(ModelInfo model) {
        if (!this.sentiment.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.sentiment.get(model);
    }
    public void deleteSentiment(ModelInfo model) {
        this.sentiment.remove(model);
    }
    public HashMap<ModelInfo, ArrayList<TopicClass>> getTopics() {
        return topics;
    }



}
