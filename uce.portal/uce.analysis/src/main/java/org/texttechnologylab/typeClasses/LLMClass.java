package org.texttechnologylab.typeClasses;

import org.texttechnologylab.modules.ModelInfo;

public class LLMClass {

    private String systemPrompt;
    private String modelName;
    private String result;
    private double duration;
    private ModelInfo modelInfo;



    public String getSystemPrompt() {
        return systemPrompt;
    }
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    public String getModelName() {
        return modelName;
    }
    public void setModelName(String modelName) {
        modelName = modelName;
    }
    public String getResult() {
        return result;
    }
    public void setResult(String result) {
        this.result = result;
    }
    public double getDuration() {
        return duration;
    }
    public void setDuration(double duration) {
        this.duration = duration;
    }
    public ModelInfo getModelInfo() {
        return modelInfo;
    }
    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

}
