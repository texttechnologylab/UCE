package org.texttechnologylab.modules;

import org.texttechnologylab.typeClasses.TextClass;

import java.util.HashMap;
import java.util.List;

public class DUUIInformation {
    private Sentences sentence;

    private TextClass textInformation;

    private List<ModelGroup> modelGroups;

    private HashMap<String, ModelInfo> modelInfos;

    private Boolean isHateSpeech = false;

    private Boolean isSentiment = false;

    private Boolean isTopic = false;

    private Boolean isToxic = false;

    private Boolean isEmotion = false;

    private Boolean isFact = false;

    private Boolean isCoherence = false;

    private Boolean isStance = false;

    private Boolean isReadability = false;

    private Boolean isTA = false;

    private Boolean isLLM = false;

    private Boolean isOffensive = false;

    public DUUIInformation(Sentences sentence, TextClass textInformation, List<ModelGroup> modelGroups, HashMap<String, ModelInfo> modelInfos) {
        this.sentence = sentence;
        this.textInformation = textInformation;
        this.modelGroups = modelGroups;
        this.modelInfos = modelInfos;
    }

    public Sentences getSentence() {
        return sentence;
    }

    public void setSentence(Sentences sentence) {
        this.sentence = sentence;
    }

    public TextClass getTextInformation() {
        return textInformation;
    }

    public void setTextInformation(TextClass textInformation) {
        this.textInformation = textInformation;
    }

    public List<ModelGroup> getModelGroups() {
        return modelGroups;
    }

    public void setModelGroups(List<ModelGroup> modelGroups) {
        this.modelGroups = modelGroups;
    }

    public HashMap<String, ModelInfo> getModelInfos() {
        return modelInfos;
    }

    public void setModelInfos(HashMap<String, ModelInfo> modelInfos) {
        this.modelInfos = modelInfos;
    }

    private ModelInfo getModelInfo(String modelKey) {
        return modelInfos.get(modelKey);
    }

    public Boolean getIsHateSpeech() {
        return isHateSpeech;
    }
    public void setIsHateSpeech(Boolean isHateSpeech) {
        this.isHateSpeech = isHateSpeech;
    }

    public Boolean getIsSentiment() {
        return isSentiment;
    }
    public void setIsSentiment(Boolean isSentiment) {
        this.isSentiment = isSentiment;
    }

    public Boolean getIsTopic() {
        return isTopic;
    }

    public void setIsTopic(Boolean isTopic) {
        this.isTopic = isTopic;
    }

    public Boolean getIsOffensive() {
        return isOffensive;
    }

    public void setIsOffensive(Boolean isOffensive) {
        this.isOffensive = isOffensive;
    }

    public Boolean getIsToxic() {
        return isToxic;
    }

    public void setIsToxic(Boolean isToxic) {
        this.isToxic = isToxic;
    }

    public Boolean getIsEmotion() {
        return isEmotion;
    }

    public void setIsEmotion(Boolean isEmotion) {
        this.isEmotion = isEmotion;
    }

    public Boolean getIsFact() {
        return isFact;
    }

    public void setIsFact(Boolean isFact) {
        this.isFact = isFact;
    }

    public Boolean getIsCoherence() {
        return isCoherence;
    }

    public void setIsCoherence(Boolean isCoherence) {
        this.isCoherence = isCoherence;
    }

    public Boolean getIsStance() {
        return isStance;
    }

    public void setIsStance(Boolean isStance) {
        this.isStance = isStance;
    }

    public Boolean getIsReadability() {
        return isReadability;
    }

    public void setIsReadability(Boolean isReadability) {
        this.isReadability = isReadability;
    }

    public Boolean getIsTA() {
        return isTA;
    }

    public void setIsTA(Boolean isTA) {
        this.isTA = isTA;
    }

    public void setIsLLM(Boolean isLLM) {
        this.isLLM = isLLM;
    }

    public Boolean getIsLLM() {
        return isLLM;
    }

}
