package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

public class FactClass {

    private Double fact;
    private Double NonFact;
    private ModelInfo modelInfo;

    private ClaimClass claim;

    public void setFact(Double fact) {
        this.fact = fact;
    }
    public Double getFact() {
        return fact;
    }
    public void setNonFact(Double nonFact) {
        NonFact = nonFact;
    }
    public Double getNonFact() {
        return NonFact;
    }

    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

    public ModelInfo getModelInfo() {
        return modelInfo;
    }

    public ClaimClass getClaim() {
        return claim;
    }

    public void setClaim(ClaimClass claim) {
        this.claim = claim;
    }

}
