package org.texttechnologylab.uce.analysis;

import org.texttechnologylab.uce.analysis.modules.DUUIInformation;
import org.texttechnologylab.uce.analysis.modules.ModelGroup;

import java.util.HashMap;
import java.util.List;

public class History {
    private HashMap<String, DUUIInformation> duuiInformationHashMap = new HashMap<>();
    private HashMap<String, ModelGroup> modelGroupHashMap = new HashMap<>();
    private HashMap<String, String>  inputTextHashMap = new HashMap<>();
    private HashMap<String, List<String>> selectedModelsHashMap = new HashMap<>();

    private HashMap<String, String> inputClaimHashMap = new HashMap<>();

    private HashMap<String, String> inputCoherenceHashMap = new HashMap<>();

    private HashMap<String, String> inputStanceHashMap = new HashMap<>();

    private HashMap<String, String> inputLLMHashMap = new HashMap<>();

    public HashMap<String, DUUIInformation> getDuuiInformationHashMap() {
        return duuiInformationHashMap;
    }

    public void setDuuiInformationHashMap(HashMap<String, DUUIInformation> duuiInformationHashMap) {
        this.duuiInformationHashMap = duuiInformationHashMap;
    }
    public HashMap<String, ModelGroup> getModelGroupHashMap() {
        return modelGroupHashMap;
    }
    public void setModelGroupHashMap(HashMap<String, ModelGroup> modelGroupHashMap) {
        this.modelGroupHashMap = modelGroupHashMap;
    }
    public HashMap<String, String> getInputTextHashMap() {
        return inputTextHashMap;
    }
    public void setInputTextHashMap(HashMap<String, String> inputTextHashMap) {
        this.inputTextHashMap = inputTextHashMap;
    }
    public HashMap<String, String> getInputClaimHashMap() {
        return inputClaimHashMap;
    }
    public void setInputClaimHashMap(HashMap<String, String> inputClaimHashMap) {
        this.inputClaimHashMap = inputClaimHashMap;
    }
    public HashMap<String, List<String>> getSelectedModelsHashMap() {
        return selectedModelsHashMap;
    }
    public void setSelectedModelsHashMap(HashMap<String, List<String>> selectedModelsHashMap) {
        this.selectedModelsHashMap = selectedModelsHashMap;
    }

    public HashMap<String, String> getInputCoherenceHashMap() {
        return inputCoherenceHashMap;
    }
    public void setInputCoherenceHashMap(HashMap<String, String> inputCoherenceHashMap) {
        this.inputCoherenceHashMap = inputCoherenceHashMap;
    }
    public HashMap<String, String> getInputStanceHashMap() {
        return inputStanceHashMap;
    }
    public void setInputStanceHashMap(HashMap<String, String> inputStanceHashMap) {
        this.inputStanceHashMap = inputStanceHashMap;
    }



    public void addDuuiInformation(String id, DUUIInformation duuiInformation) {
        this.duuiInformationHashMap.put(id, duuiInformation);
    }

    public void addInputStance(String id, String inputStance) {
        this.inputStanceHashMap.put(id, inputStance);
    }

    public void addModelGroup(String id, ModelGroup modelGroup) {
        this.modelGroupHashMap.put(id, modelGroup);
    }

    public void addInputText(String id, String inputText) {
        this.inputTextHashMap.put(id, inputText);
    }

    public void addInputClaim(String id, String inputClaim) {
        this.inputClaimHashMap.put(id, inputClaim);
    }

   public void addInputLLM(String id, String inputLLM) {
        this.inputLLMHashMap.put(id, inputLLM);
    }

    public void addSelectedModels(String id, List<String> selectedModels) {
        this.selectedModelsHashMap.put(id, selectedModels);
    }
    public void addInputCoherence(String id, String inputCoherence) {
        this.inputCoherenceHashMap.put(id, inputCoherence);
    }
    public DUUIInformation getDuuiInformation(String id) {
        return this.duuiInformationHashMap.get(id);
    }
    public ModelGroup getModelGroup(String id) {
        return this.modelGroupHashMap.get(id);
    }
    public String getInputText(String id) {
        return this.inputTextHashMap.get(id);
    }

    public String getInputClaim(String id) {
        return this.inputClaimHashMap.get(id);
    }

   public String getInputLLM(String id) {
        return this.inputLLMHashMap.get(id);
    }

    public List<String> getSelectedModels(String id) {
        return this.selectedModelsHashMap.get(id);
    }

    public String getInputCoherence(String id) {
        return this.inputCoherenceHashMap.get(id);
    }

    public String getInputStance(String id) {
        return this.inputStanceHashMap.get(id);
    }

    public void removeDuuiInformation(String id) {
        this.duuiInformationHashMap.remove(id);
    }

    public void removeModelGroup(String id) {
        this.modelGroupHashMap.remove(id);
    }

    public void removeInputText(String id) {
        this.inputTextHashMap.remove(id);
    }

    public void removeSelectedModels(String id) {
        this.selectedModelsHashMap.remove(id);
    }

    public List<String> getAllKeys() {
        return this.selectedModelsHashMap.keySet().stream().toList();
    }

    public void setModelGroupHashMap(String id, List<ModelGroup> modelGroups) {
        for (ModelGroup modelGroup : modelGroups) {
            this.modelGroupHashMap.put(id, modelGroup);
        }
    }
}
