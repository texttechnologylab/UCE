package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

import java.util.ArrayList;

public class TAClass {

    private String groupName;
    private ArrayList<TAInput> taInputs = new ArrayList<>();
    private ModelInfo modelInfo;

    public String getGroupName() {
        return groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    public ArrayList<TAInput> getTaInputs() {
        return taInputs;
    }
    public void setTaInputs(ArrayList<TAInput> taInputs) {
        this.taInputs = taInputs;
    }
    public void addTaInput(TAInput taInput) {
        this.taInputs.add(taInput);
    }
    public ModelInfo getModelInfo() {
        return modelInfo;
    }
    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

}
