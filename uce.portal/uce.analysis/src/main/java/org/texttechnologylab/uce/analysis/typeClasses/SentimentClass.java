package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

public class SentimentClass {

    private Double Positive;
    private Double Negative;
    private Double Neutral;

    private ModelInfo modelInfo;

    public void setPositive(Double positive) {
        Positive = positive;
    }

    public Double getPositive() {
        return Positive;
    }

    public void setNegative(Double negative) {
        Negative = negative;
    }

    public Double getNegative() {
        return Negative;
    }

    public void setNeutral(Double neutral) {
        Neutral = neutral;
    }

    public Double getNeutral() {
        return Neutral;
    }

    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

    public ModelInfo getModelInfo() {
        return modelInfo;
    }
}
