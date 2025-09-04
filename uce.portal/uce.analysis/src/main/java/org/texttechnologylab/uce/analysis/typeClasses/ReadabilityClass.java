package org.texttechnologylab.uce.analysis.typeClasses;

import org.texttechnologylab.uce.analysis.modules.ModelInfo;

import java.util.ArrayList;

public class ReadabilityClass {
    private String groupName;
    private ArrayList<ReadabilityInput> readabilityInputs = new ArrayList<>();
    private double FleschKincaid;
    private double Flesch;
    private double GunningFog;
    private double ColemanLiau;
    private double DaleChall;
    private double ARI;
    private double LinsearWrite;
    private double SMOG;
    private double Spache;

    private ModelInfo modelInfo;

    public String getGroupName() {
        return groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    public ArrayList<ReadabilityInput> getReadabilityInputs() {
        return readabilityInputs;
    }
    public void setReadabilityInputs(ArrayList<ReadabilityInput> readabilityInputs) {
        this.readabilityInputs = readabilityInputs;
    }
    public void addReadabilityInput(ReadabilityInput readabilityInput) {
        this.readabilityInputs.add(readabilityInput);
    }



    public double getFleschKincaid() {
        return FleschKincaid;
    }
    public void setFleschKincaid(double fleschKincaid) {
        FleschKincaid = fleschKincaid;
    }
    public double getFlesch() {
        return Flesch;
    }
    public void setFlesch(double flesch) {
        Flesch = flesch;
    }
    public double getGunningFog() {
        return GunningFog;
    }
    public void setGunningFog(double gunningFog) {
        GunningFog = gunningFog;
    }
    public double getColemanLiau() {
        return ColemanLiau;
    }
    public void setColemanLiau(double colemanLiau) {
        ColemanLiau = colemanLiau;
    }
    public double getDaleChall() {
        return DaleChall;
    }
    public void setDaleChall(double daleChall) {
        DaleChall = daleChall;
    }
    public double getARI() {
        return ARI;
    }
    public void setARI(double aRI) {
        ARI = aRI;
    }
    public double getLinsearWrite() {
        return LinsearWrite;
    }
    public void setLinsearWrite(double linsearWrite) {
        LinsearWrite = linsearWrite;
    }
    public double getSMOG() {
        return SMOG;
    }
    public void setSMOG(double sMOG) {
        SMOG = sMOG;
    }
    public double getSpache() {
        return Spache;
    }
    public void setSpache(double spache) {
        Spache = spache;
    }
    public ModelInfo getModelInfo() {
        return modelInfo;
    }
    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }
}
