package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

public class HateClass {

    private Double Hate;
    private Double NonHate;

    private ModelInfo modelInfo;

    public void setHate(Double hate) {
        Hate = hate;
    }

    public Double getHate() {
        return Hate;
    }

    public void setNonHate(Double nonHate) {
        NonHate = nonHate;
    }

    public Double getNonHate() {
        return NonHate;
    }

    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

    public ModelInfo getModelInfo() {
        return modelInfo;
    }
}
