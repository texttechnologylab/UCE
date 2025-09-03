package org.texttechnologylab.uce.analysis.modules;
import java.util.ArrayList;
import java.util.List;

public class ModelGroup {
    private String name;
    private List<ModelInfo> models = new ArrayList<>();

    public ModelGroup(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public List<ModelInfo> getModels() { return models; }

    public void addModel(ModelInfo model) {
        this.models.add(model);
    }
}
