package org.texttechnologylab.uce.common.models.geonames;

public class FeatureCode {

    private String featureClass;
    private String featureCode;
    private String name;
    private String description;

    public static FeatureCode fromFileLine(String line){
        var code = new FeatureCode();
        var split = line.split("\t");
        if(split[0].equals("null")) return null;
        code.setFeatureClass(split[0].split("\\.")[0]);
        code.setFeatureCode(split[0].split("\\.")[1]);
        code.setName(split.length > 1 ? split[1] : "");
        code.setDescription(split.length > 2 ? split[2] : "");
        return code;
    }

    public FeatureCode(){}

    public String getFeatureClass() {
        return featureClass;
    }

    public void setFeatureClass(String featureClass) {
        this.featureClass = featureClass;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
