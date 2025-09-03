package org.texttechnologylab.uce.common.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.texttechnologylab.uce.common.models.viewModels.JsonViewModel;

import java.util.ArrayList;
import java.util.List;

public class JsonBeautifier {

    public List<JsonViewModel> parseJsonToViewModel(String jsonString) {
        var jsonElement = JsonParser.parseString(jsonString);
        return parseElement(null, jsonElement);
    }

    private List<JsonViewModel> parseElement(String parentKey, JsonElement element) {
        List<JsonViewModel> models = new ArrayList<>();

        if (element.isJsonObject()) {
            // Handle JSON object
            var jsonObject = element.getAsJsonObject();
            for (var entry : jsonObject.entrySet()) {
                var childModel = new JsonViewModel();
                childModel.setKey(entry.getKey());
                childModel.setValueType("object");
                childModel.setChildren(parseElement(entry.getKey(), entry.getValue())); // Recursively parse children
                models.add(childModel);
            }
        } else if (element.isJsonArray()) {
            // Handle JSON array
            var jsonArray = element.getAsJsonArray();
            int index = 0;
            for (var item : jsonArray) {
                var childModel = new JsonViewModel();
                childModel.setKey((parentKey != null ? parentKey : "array") + "[" + index + "]");
                childModel.setValueType("array");
                childModel.setChildren(parseElement(null, item)); // Recursively parse children
                models.add(childModel);
                index++;
            }
        } else if (element.isJsonPrimitive()) {
            // Handle primitive values directly as a leaf node
            var primitive = element.getAsJsonPrimitive();
            var leafModel = new JsonViewModel();
            leafModel.setKey(parentKey); // Directly assign the parent key
            if (primitive.isString()) {
                leafModel.setValueType("string");
                leafModel.setValue(primitive.getAsString());
            } else if (primitive.isNumber()) {
                leafModel.setValueType("number");
                leafModel.setValue(primitive.getAsString());
            } else if (primitive.isBoolean()) {
                leafModel.setValueType("boolean");
                leafModel.setValue(String.valueOf(primitive.getAsBoolean()));
            }
            models.add(leafModel); // Add the leaf model directly without nesting
        } else if (element.isJsonNull()) {
            // Handle null values directly as a leaf node
            var nullModel = new JsonViewModel();
            nullModel.setKey(parentKey); // Directly assign the parent key
            nullModel.setValueType("null");
            nullModel.setValue(null);
            models.add(nullModel); // Add the null model directly without nesting
        }

        return models;
    }

}
