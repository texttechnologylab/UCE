package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

public class CoherenceClass {

    private int begin;
    private int end;

    private float euclidean;
    private float cosine;
    private float wasserstein;
    private float distanceCorrelation;
    private float jensenshannon;
    private float bhattacharyya;

    private CoherenceSentence coherenceSentence;

    private ModelInfo modelInfo;
    public float getEuclidean() {
        return euclidean;
    }
    public void setEuclidean(float euclidean) {
        this.euclidean = euclidean;
    }
    public float getCosine() {
        return cosine;
    }
    public void setCosine(float cosine) {
        this.cosine = cosine;
    }
    public float getWasserstein() {
        return wasserstein;
    }
    public void setWasserstein(float wasserstein) {
        this.wasserstein = wasserstein;
    }
    public float getDistanceCorrelation() {
        return distanceCorrelation;
    }
    public void setDistanceCorrelation(float distanceCorrelation) {
        this.distanceCorrelation = distanceCorrelation;
    }
    public float getJensenshannon() {
        return jensenshannon;
    }
    public void setJensenshannon(float jensenshannon) {
        this.jensenshannon = jensenshannon;
    }
    public float getBhattacharyya() {
        return bhattacharyya;
    }
    public void setBhattacharyya(float bhattacharyya) {
        this.bhattacharyya = bhattacharyya;
    }

    public CoherenceSentence getCoherenceSentence() {
        return coherenceSentence;
    }

    public void setCoherenceSentence(CoherenceSentence coherenceSentence) {
        this.coherenceSentence = coherenceSentence;
    }
    public ModelInfo getModelInfo() {
        return modelInfo;
    }
    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

    public int getBegin() {
        return begin;
    }
    public void setBegin(int begin) {
        this.begin = begin;
    }
    public int getEnd() {
        return end;
    }
    public void setEnd(int end) {
        this.end = end;
    }

}
