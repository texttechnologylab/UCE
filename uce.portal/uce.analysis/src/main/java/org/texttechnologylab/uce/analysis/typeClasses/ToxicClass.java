package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

public class ToxicClass {

    private Double toxic;
    private Double NonToxic;

    private ModelInfo modelInfo;

    public void setToxic(Double toxic) {
        this.toxic = toxic;
    }
    public Double getToxic() {
        return toxic;
    }
    public void setNonToxic(Double nonToxic) {
        this.NonToxic = nonToxic;
    }

    public Double getNonToxic() {
        return NonToxic;
    }

    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

    public ModelInfo getModelInfo() {
        return modelInfo;
    }
}
