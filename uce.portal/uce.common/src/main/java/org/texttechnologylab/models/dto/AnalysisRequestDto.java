package org.texttechnologylab.models.dto;

import java.util.List;

/**
 * DTO für das Parsen der Analyse-Request-Daten
 */
public class AnalysisRequestDto {

    private List<String> selectedModels;
    private String inputText;

    private String inputClaim;

    private String inputCoherence;

    private String inputStance;

    private String inputSystemPrompt;

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

    public String getInputClaim() {
        return inputClaim;
    }

    public void setInputClaim(String inputClaim) {
        this.inputClaim = inputClaim;
    }

    public String getInputCoherence() {
        return inputCoherence;
    }

    public void setInputCoherence(String inputCoherence) {
        this.inputCoherence = inputCoherence;
    }

    public String getInputStance() {
        return inputStance;
    }

    public void setInputStance(String inputStance) {
        this.inputStance = inputStance;
    }

    public String getInputSystemPrompt() {
        return inputSystemPrompt;
    }
    public void setInputSystemPrompt(String inputSystemPrompt) {
        this.inputSystemPrompt = inputSystemPrompt;
    }
}
