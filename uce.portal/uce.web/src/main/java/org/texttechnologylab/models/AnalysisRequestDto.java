package org.texttechnologylab.models;

import java.util.List;

/**
 * DTO für das Parsen der Analyse-Request-Daten
 */
public class AnalysisRequestDto {

    private List<String> selectedModels;
    private String inputText;

    public AnalysisRequestDto() {
        // Empty constructor for Gson
    }

    public List<String> getSelectedModels() {
        return selectedModels;
    }

    public void setSelectedModels(List<String> selectedModels) {
        this.selectedModels = selectedModels;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }
}
