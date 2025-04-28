package org.texttechnologylab;

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

}
