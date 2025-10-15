package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

import java.util.ArrayList;

public class CohMetrixClass {

    private String groupName;
    private ArrayList<CohMetrixInput> CohMetrixInputs = new ArrayList<>();
    private ModelInfo modelInfo;

    public String getGroupName() {
        return groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    public ArrayList<CohMetrixInput> getCohMetrixInputs() {
        return CohMetrixInputs;
    }
    public void setCohMetrixInputs(ArrayList<CohMetrixInput> CohMetrixInputs) {
        this.CohMetrixInputs = CohMetrixInputs;
    }
    public void addCohMetrixInput(CohMetrixInput input) {
        this.CohMetrixInputs.add(input);
    }
    public ModelInfo getModelInfo() {
        return modelInfo;
    }
    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

}
