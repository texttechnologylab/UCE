package org.texttechnologylab.uce.analysis.modules;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public final class ModelResources {

    private final Document modelResources;
    private HashMap<String, ModelInfo> groupMap;

    public void setGroupMap(HashMap<String, ModelInfo> groupMap) {
        this.groupMap = groupMap;
    }

    public HashMap<String, ModelInfo> getGroupMap() {
        return groupMap;
    }

    public ModelResources() throws IOException {
        var inputStream = getClass().getClassLoader().getResourceAsStream("models.json");
        String jsonData;
        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            jsonData = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        var gson = new Gson();
        this.modelResources = gson.fromJson(jsonData, Document.class);
    }

    public List<ModelGroup> getGroupedModelObjects() {
        Map<String, ModelGroup> groupMap = new LinkedHashMap<>();
        HashMap<String, ModelInfo> groupMap2 = new HashMap<>();
        for (Map.Entry<String, Object> entry : modelResources.entrySet()) {
            String modelKey = entry.getKey();
            LinkedTreeMap<String, String> modelData = (LinkedTreeMap<String, String>) entry.getValue();

            String mainTool = modelData.get("Main Tool");
            if (mainTool == null || mainTool.isBlank()) {
                mainTool = "Uncategorized";
            }

            ModelInfo model = new ModelInfo();
            model.setKey(modelKey);
            model.setName(modelData.get("Name"));
            model.setUrl(modelData.get("url"));
            model.setGithub(modelData.get("github"));
            model.setHuggingface(modelData.get("huggingface"));
            model.setPaper(modelData.get("paper"));
            model.setMap(modelData.get("map"));
            model.setVariant(modelData.get("Variant"));
            model.setModelType(modelData.get("type"));
            model.setUrlParameter(modelData.getOrDefault("url-Parameter", ""));
            model.setPortParameter(modelData.getOrDefault("port-Parameter", "0"));
            model.setMainTool(mainTool);
            String modelkeyName = mainTool.replace(" ", "_")+"_"+modelKey.replace(" ", "_");
            groupMap2.put(modelkeyName, model);
            groupMap.computeIfAbsent(mainTool, ModelGroup::new).addModel(model);
        }
        this.groupMap = groupMap2;
        return new ArrayList<>(groupMap.values());
    }
}
