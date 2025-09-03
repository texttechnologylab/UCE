package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

public class StanceClass {

    private HypothesisClass hypothesis;
    private double support;
    private double oppose;
    private double neutral;
    private ModelInfo modelInfo;

    public HypothesisClass getHypothesis() {
        return hypothesis;
    }

    public void setHypothesis(HypothesisClass hypothesis) {
        this.hypothesis = hypothesis;
    }

    public double getSupport() {
        return support;
    }

    public void setSupport(double support) {
        this.support = support;
    }

    public double getOppose() {
        return oppose;
    }

    public void setOppose(double oppose) {
        this.oppose = oppose;
    }

    public double getNeutral() {
        return neutral;
    }

    public void setNeutral(double neutral) {
        this.neutral = neutral;
    }

    public ModelInfo getModelInfo() {
        return modelInfo;
    }

    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }
}
