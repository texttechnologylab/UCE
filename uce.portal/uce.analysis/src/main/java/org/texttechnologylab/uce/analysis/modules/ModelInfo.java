package org.texttechnologylab.uce.analysis.modules;



public class ModelInfo {
    private String key;
    private String name;
    private String url;
    private String github;
    private String huggingface;
    private String paper;
    private String map;

    private String variant;
    private String mainTool;

    private String modelType;

    private String urlParameter;
    private String portParameter;

    // Getter & Setter
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getGithub() {
        return github;
    }

    public void setGithub(String github) {
        this.github = github;
    }

    public String getHuggingface() {
        return huggingface;
    }

    public void setHuggingface(String huggingface) {
        this.huggingface = huggingface;
    }

    public String getPaper() {
        return paper;
    }

    public void setPaper(String paper) {
        this.paper = paper;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getMainTool() {
        return mainTool;
    }

    public void setMainTool(String mainTool) {
        this.mainTool = mainTool;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getUrlParameter() {
        return urlParameter;
    }
    public void setUrlParameter(String urlParameter) {
        this.urlParameter = urlParameter;
    }
    public String getPortParameter() {
        return portParameter;
    }
    public void setPortParameter(String portParameter) {
        this.portParameter = portParameter;
    }

}
