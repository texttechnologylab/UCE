package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

import java.util.ArrayList;

public class OffensiveClass {

    private ArrayList<OffensiveInput> offensives = new ArrayList<>();
    private ModelInfo modelInfo;

    public void setOffensives(ArrayList<OffensiveInput> offensives) {
        this.offensives = offensives;
    }

    public void addOffensive(OffensiveInput offensive) {
        this.offensives.add(offensive);
    }

    public ArrayList<OffensiveInput> getOffensives() {
        return offensives;
    }

    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

    public ModelInfo getModelInfo() {
        return modelInfo;
    }
}
