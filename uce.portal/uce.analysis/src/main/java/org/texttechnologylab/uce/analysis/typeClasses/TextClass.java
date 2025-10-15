package org.texttechnologylab.uce.analysis.typeClasses;
import org.texttechnologylab.uce.analysis.modules.ModelInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class TextClass {

    private LinkedHashMap<ModelInfo, ArrayList<TopicClass>> topics = new LinkedHashMap<>();
    private LinkedHashMap<ModelInfo, ArrayList<OffensiveClass>> offensives = new LinkedHashMap<>();
    private LinkedHashMap<ModelInfo, ArrayList<HateClass>> hate = new LinkedHashMap<>();
    private LinkedHashMap<ModelInfo, ArrayList<SentimentClass>> sentiment = new LinkedHashMap<>();
    private LinkedHashMap<ModelInfo, ArrayList<ToxicClass>> toxic = new LinkedHashMap<>();
    private LinkedHashMap<ModelInfo, ArrayList<EmotionClass>> emotions = new LinkedHashMap<>();

    private LinkedHashMap<ModelInfo, ArrayList<FactClass>> facts = new LinkedHashMap<>();

    private LinkedHashMap<ModelInfo, ArrayList<CoherenceClass>> coherence = new LinkedHashMap<>();
    private LinkedHashMap<ModelInfo, ArrayList<StanceClass>> stance = new LinkedHashMap<>();
    private LinkedHashMap<ModelInfo, LLMClass> llm = new LinkedHashMap<>();

    private ArrayList<HateClass> hateAVG = new ArrayList<>();
    private ArrayList<SentimentClass> sentimentAVG = new ArrayList<>();
    private ArrayList<TopicClass> topicAVG = new ArrayList<>();
    private ArrayList<OffensiveClass> offensiveAVG = new ArrayList<>();
    private ArrayList<ToxicClass> toxicAVG = new ArrayList<>();
    private ArrayList<EmotionClass> emotionAVG = new ArrayList<>();
    private ArrayList<FactClass> factAVG = new ArrayList<>();

    private ArrayList<CoherenceClass> coherenceAVG = new ArrayList<>();
    private ArrayList<StanceClass> stanceAVG = new ArrayList<>();
    private ArrayList<ReadabilityClass> readabilityAVG = new ArrayList<>();
    private ArrayList<LLMClass> llmAVG = new ArrayList<>();
    private ArrayList<TAClass> taScoreAVG = new ArrayList<>();
    private ArrayList<CohMetrixClass> cohMetrixAVG = new ArrayList<>();

    private ArrayList<ModelInfo> topicsModels = new ArrayList<>();
    private ArrayList<ModelInfo> offensivesModels = new ArrayList<>();
    private ArrayList<ModelInfo> hateModels = new ArrayList<>();
    private ArrayList<ModelInfo> sentimentModels = new ArrayList<>();
    private ArrayList<ModelInfo> toxicModels = new ArrayList<>();
    private ArrayList<ModelInfo> emotionModels = new ArrayList<>();

    private ArrayList<ModelInfo> factModels = new ArrayList<>();
    private ArrayList<ModelInfo> stanceModels = new ArrayList<>();

    private ArrayList<ModelInfo> coherenceModels = new ArrayList<>();
    private ArrayList<ModelInfo> readabilityModels = new ArrayList<>();
    private ArrayList<ModelInfo> taModels = new ArrayList<>();
    private ArrayList<ModelInfo> cohMetrixModels = new ArrayList<>();
    private ArrayList<ModelInfo> llmModels = new ArrayList<>();

    private ClaimClass claim = new ClaimClass();

    private CoherenceSentence coherenceSentence = new CoherenceSentence();
    private HypothesisClass hypothesis = new HypothesisClass();

    public void addTopic(ModelInfo model, TopicClass topic) {
        if (!this.topics.containsKey(model)) {
            this.topics.put(model, new ArrayList<>());
        }
        this.topics.get(model).add(topic);
        if (!this.topicsModels.contains(model)) {
            this.topicsModels.add(model);
        }
    }

    public void addOffensive(ModelInfo model, OffensiveClass offensive) {
        if (!this.offensives.containsKey(model)) {
            this.offensives.put(model, new ArrayList<>());
        }
        this.offensives.get(model).add(offensive);
        if (!this.offensivesModels.contains(model)) {
            this.offensivesModels.add(model);
        }
    }

    public void addEmotion(ModelInfo model, EmotionClass emotion) {
        if (!this.emotions.containsKey(model)) {
            this.emotions.put(model, new ArrayList<>());
        }
        this.emotions.get(model).add(emotion);
        if (!this.emotionModels.contains(model)) {
            this.emotionModels.add(model);
        }
    }

    public void addFact(ModelInfo model, FactClass fact) {
        if (!this.facts.containsKey(model)) {
            this.facts.put(model, new ArrayList<>());
        }
        this.facts.get(model).add(fact);
        if (!this.factModels.contains(model)) {
            this.factModels.add(model);
        }
    }

    public void addCoherence(ModelInfo model, CoherenceClass coherence) {
        if (!this.coherence.containsKey(model)) {
            this.coherence.put(model, new ArrayList<>());
        }
        this.coherence.get(model).add(coherence);
        if (!this.coherenceModels.contains(model)) {
            this.coherenceModels.add(model);
        }
    }

    public void addStance(ModelInfo model, StanceClass stance) {
        if (!this.stance.containsKey(model)) {
            this.stance.put(model, new ArrayList<>());
        }
        this.stance.get(model).add(stance);
        if (!this.stanceModels.contains(model)) {
            this.stanceModels.add(model);
        }
    }

    public void addLLM(ModelInfo model, LLMClass llm) {
        this.llm.put(model, llm);
        if (!this.llmModels.contains(model)) {
            this.llmModels.add(model);
        }
    }

    public ArrayList<TopicClass> getTopic(ModelInfo model) {
        if (!this.topics.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.topics.get(model);
    }

    public ArrayList<OffensiveClass> getOffensive(ModelInfo model) {
        if (!this.offensives.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.offensives.get(model);
    }

    public ArrayList<EmotionClass> getEmotion(ModelInfo model) {
        if (!this.emotions.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.emotions.get(model);
    }

    public void deleteTopic(ModelInfo model) {
        this.topics.remove(model);
        this.topicsModels.remove(model);
    }

    public void deleteEmotion(ModelInfo model) {
        this.emotions.remove(model);
        this.emotionModels.remove(model);
    }

    public void deleteFact(ModelInfo model) {
        this.facts.remove(model);
        this.factModels.remove(model);
    }

    public ArrayList<FactClass> getFact(ModelInfo model) {
        if (!this.facts.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.facts.get(model);
    }

    public ArrayList<StanceClass> getStance(ModelInfo model) {
        if (!this.stance.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.stance.get(model);
    }
    public void deleteStance(ModelInfo model) {
        this.stance.remove(model);
        this.stanceModels.remove(model);
    }

    public ArrayList<CoherenceClass> getCoherence(ModelInfo model) {
        if (!this.coherence.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.coherence.get(model);
    }
    public void deleteCoherence(ModelInfo model) {
        this.coherence.remove(model);
        this.coherenceModels.remove(model);
    }


    public void addHate(ModelInfo model, HateClass hate) {
        if (!this.hate.containsKey(model)) {
            this.hate.put(model, new ArrayList<>());
        }
        this.hate.get(model).add(hate);
        if (!this.hateModels.contains(model)) {
            this.hateModels.add(model);
        }
    }

    public ArrayList<HateClass> getHate(ModelInfo model) {
        if (!this.hate.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.hate.get(model);
    }

    public void deleteHate(ModelInfo model) {
        this.hate.remove(model);
        this.hateModels.remove(model);
    }
    public void addSentiment(ModelInfo model, SentimentClass sentiment) {
        if (!this.sentiment.containsKey(model)) {
            this.sentiment.put(model, new ArrayList<>());
        }
        this.sentiment.get(model).add(sentiment);
        if (!this.sentimentModels.contains(model)) {
            this.sentimentModels.add(model);
        }
    }
    public ArrayList<SentimentClass> getSentiment(ModelInfo model) {
        if (!this.sentiment.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.sentiment.get(model);
    }
    public void deleteSentiment(ModelInfo model) {
        this.sentiment.remove(model);
        this.sentimentModels.remove(model);
    }

    public void addToxic(ModelInfo model, ToxicClass toxic) {
        if (!this.toxic.containsKey(model)) {
            this.toxic.put(model, new ArrayList<>());
        }
        this.toxic.get(model).add(toxic);
        if (!this.toxicModels.contains(model)) {
            this.toxicModels.add(model);
        }
    }
    public void addReadability(ReadabilityClass readability) {
        this.readabilityAVG.add(readability);
    }
    public ArrayList<ReadabilityClass> getReadabilityAVG() {
        return readabilityAVG;
    }

    public ArrayList<ToxicClass> getToxic(ModelInfo model) {
        if (!this.toxic.containsKey(model)) {
            return new ArrayList<>();
        }
        return this.toxic.get(model);
    }

    public void deleteToxic(ModelInfo model) {
        this.toxic.remove(model);
        this.toxicModels.remove(model);
    }

    public void addAVGLLM(LLMClass llm) {
        this.llmAVG.add(llm);
    }

    public void addAVGTA(TAClass ta) {
        this.taScoreAVG.add(ta);
    }

    public void addAVGCohMetrix(CohMetrixClass cohMetrix) {
        this.cohMetrixAVG.add(cohMetrix);
    }



    public HashMap<ModelInfo, ArrayList<TopicClass>> getTopics() {
        return topics;
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
            avgHate.setModelInfo(model);
            this.hateAVG.add(avgHate);
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
            avgSentiment.setModelInfo(model);
            this.sentimentAVG.add(avgSentiment);
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
            avgTopic.setModelInfo(model);
            this.topicAVG.add(avgTopic);
        }
    }

    public void computeAVGOffensive(){
        for (ModelInfo model : this.offensives.keySet()) {
            HashMap<String, Double> offensiveScores = new HashMap<>();
            ArrayList<OffensiveClass> offensiveList = this.offensives.get(model);
            for (OffensiveClass offensive : offensiveList) {
                ArrayList<OffensiveInput> offensives = offensive.getOffensives();
                for (OffensiveInput offensiveInput : offensives) {
                    String key = offensiveInput.getKey();
                    double score = offensiveInput.getScore();
                    offensiveScores.put(key, offensiveScores.getOrDefault(key, 0.0) + score);
                }
            }
            OffensiveClass avgOffensive = new OffensiveClass();
            ArrayList<OffensiveInput> avgOffensives = new ArrayList<>();
            for (String key : offensiveScores.keySet()) {
                OffensiveInput offensiveInput = new OffensiveInput();
                offensiveInput.setKey(key);
                offensiveInput.setScore(offensiveScores.get(key) / offensiveList.size());
                avgOffensives.add(offensiveInput);
            }
            avgOffensive.setOffensives(avgOffensives);
            avgOffensive.setModelInfo(model);
            this.offensiveAVG.add(avgOffensive);
        }
    }

    public void computeAVGEmotion(){
        for (ModelInfo model : this.emotions.keySet()) {
            HashMap<String, Double> emotionScores = new HashMap<>();
            ArrayList<EmotionClass> emotionList = this.emotions.get(model);
            for (EmotionClass emotion : emotionList) {
                ArrayList<EmotionInput> emotions = emotion.getEmotions();
                for (EmotionInput emotionInput : emotions) {
                    String key = emotionInput.getKey();
                    double score = emotionInput.getScore();
                    emotionScores.put(key, emotionScores.getOrDefault(key, 0.0) + score);
                }
            }
            EmotionClass avgEmotion = new EmotionClass();
            ArrayList<EmotionInput> avgEmotions = new ArrayList<>();
            for (String key : emotionScores.keySet()) {
                EmotionInput emotionInput = new EmotionInput();
                emotionInput.setKey(key);
                emotionInput.setScore(emotionScores.get(key) / emotionList.size());
                avgEmotions.add(emotionInput);
            }
            avgEmotion.setEmotions(avgEmotions);
            avgEmotion.setModelInfo(model);
            this.emotionAVG.add(avgEmotion);
        }
    }

    public void computeAVGToxic(){
        for (ModelInfo model : this.toxic.keySet()) {
            ArrayList<ToxicClass> toxicList = this.toxic.get(model);
            double totalToxic = 0;
            double totalNonToxic = 0;
            for (ToxicClass toxic : toxicList) {
                totalToxic += toxic.getToxic();
                totalNonToxic += toxic.getNonToxic();
            }
            ToxicClass avgToxic = new ToxicClass();
            avgToxic.setToxic(totalToxic / toxicList.size());
            avgToxic.setNonToxic(totalNonToxic / toxicList.size());
            avgToxic.setModelInfo(model);
            this.toxicAVG.add(avgToxic);
        }
    }

    public void computeAVGStance(){
        for (ModelInfo model : this.stance.keySet()) {
            ArrayList<StanceClass> stanceList = this.stance.get(model);
            double oppose = 0;
            double support = 0;
            double neutral = 0;
            for (StanceClass stance : stanceList) {
                oppose += stance.getOppose();
                support += stance.getSupport();
                neutral += stance.getNeutral();
            }
            StanceClass avgStance = new StanceClass();
            avgStance.setOppose(oppose / stanceList.size());
            avgStance.setSupport(support / stanceList.size());
            avgStance.setNeutral(neutral / stanceList.size());
            avgStance.setModelInfo(model);
            this.stanceAVG.add(avgStance);
        }
    }

    public void computeAVGFact(){
        for (ModelInfo model : this.facts.keySet()) {
            ArrayList<FactClass> factList = this.facts.get(model);
            double totalFact = 0;
            double totalNonFact = 0;
            for (FactClass fact : factList) {
                totalFact += fact.getFact();
                totalNonFact += fact.getNonFact();
            }
            FactClass avgFact = new FactClass();
            avgFact.setFact(totalFact / factList.size());
            avgFact.setNonFact(totalNonFact / factList.size());
            avgFact.setModelInfo(model);
            this.factAVG.add(avgFact);
        }
    }

    public void computeAVGCoherence(){
        for (ModelInfo model : this.coherence.keySet()) {
            ArrayList<CoherenceClass> coherenceList = this.coherence.get(model);
            float euclidean = 0;
            float cosine = 0;
            float distanceCorrelation = 0;
            float jensenshannon = 0;
            float bhattacharyya = 0;
            for (CoherenceClass coherence : coherenceList) {
                euclidean += coherence.getEuclidean();
                cosine += coherence.getCosine();
                distanceCorrelation += coherence.getDistanceCorrelation();
                jensenshannon += coherence.getJensenshannon();
                bhattacharyya += coherence.getBhattacharyya();
            }
            CoherenceClass avgCoherence = new CoherenceClass();
            avgCoherence.setEuclidean(euclidean / coherenceList.size());
            avgCoherence.setCosine(cosine / coherenceList.size());
            avgCoherence.setDistanceCorrelation(distanceCorrelation / coherenceList.size());
            avgCoherence.setJensenshannon(jensenshannon / coherenceList.size());
            avgCoherence.setBhattacharyya(bhattacharyya / coherenceList.size());
            avgCoherence.setModelInfo(model);
            this.coherenceAVG.add(avgCoherence);
        }
    }

    public ArrayList<HateClass> getHateAVG() {
        return hateAVG;
    }

    public ArrayList<SentimentClass> getSentimentAVG() {
        return sentimentAVG;
    }

    public ArrayList<TopicClass> getTopicAVG() {
        return topicAVG;
    }

    public ArrayList<OffensiveClass> getOffensiveAVG() {
        return offensiveAVG;
    }

    public ArrayList<LLMClass> getLlmAVG() {
        return llmAVG;
    }

    public ArrayList<TAClass> getTaScoreAVG() {
        return taScoreAVG;
    }

    public ArrayList<CohMetrixClass> getCohMetrixAVG() {
        return cohMetrixAVG;
    }

    public ArrayList<ToxicClass> getToxicAVG() {
        return toxicAVG;
    }

    public ArrayList<EmotionClass> getEmotionAVG() {
        return emotionAVG;
    }

    public ArrayList<FactClass> getFactAVG() {
        return factAVG;
    }

    public ArrayList<CoherenceClass> getCoherenceAVG() {
        return coherenceAVG;
    }

    public ArrayList<StanceClass> getStanceAVG() {
        return stanceAVG;
    }

    public ClaimClass getClaim() {
        return claim;
    }

    public void setClaim(ClaimClass claim) {
        this.claim = claim;
    }

    public CoherenceSentence getCoherenceSentence() {
        return coherenceSentence;
    }

    public void setCoherenceSentence(CoherenceSentence coherenceSentence) {
        this.coherenceSentence = coherenceSentence;
    }

    public HypothesisClass getHypothesis() {
        return hypothesis;
    }

    public void setHypothesis(HypothesisClass hypothesis) {
        this.hypothesis = hypothesis;
    }

    public ArrayList<ModelInfo> getReadabilityModels() {
        return readabilityModels;
    }

    public void setReadabilityModels(ArrayList<ModelInfo> readabilityModels) {
        this.readabilityModels = readabilityModels;
    }

    public ArrayList<ModelInfo> getLLModels() {
        return llmModels;
    }

    public void setLLModels(ArrayList<ModelInfo> llmModels) {
        this.llmModels = llmModels;
    }

    public ArrayList<ModelInfo> getTaModels() {
        return taModels;
    }
    public void setTaModels(ArrayList<ModelInfo> taModels) {
        this.taModels = taModels;
    }

    public ArrayList<ModelInfo> getCohMetrixModels() {
        return cohMetrixModels;
    }

    public void setCohMetrixModels(ArrayList<ModelInfo> cohMetrixModels) {
        this.cohMetrixModels = cohMetrixModels;
    }


}
