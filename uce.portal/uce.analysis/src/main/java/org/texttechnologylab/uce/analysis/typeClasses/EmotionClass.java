package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

import java.util.ArrayList;

public class EmotionClass {

    private ArrayList<EmotionInput> emotions = new ArrayList<>();
    private ModelInfo modelInfo;

    public void setEmotions(ArrayList<EmotionInput> emotions) {
        this.emotions = emotions;
    }

    public void addEmotion(EmotionInput emotion) {
        this.emotions.add(emotion);
    }
    public ArrayList<EmotionInput> getEmotions() {
        return emotions;
    }
    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }
    public ModelInfo getModelInfo() {
        return modelInfo;
    }
}

