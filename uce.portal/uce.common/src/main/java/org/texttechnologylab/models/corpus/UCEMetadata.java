package org.texttechnologylab.models.corpus;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.viewModels.UCEMetadataJsonViewModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ucemetadata")
public class UCEMetadata extends ModelBase {
    @Column(name = "document_id", insertable = false, updatable = false)
    private long documentId;
    private String key;
    @Column(columnDefinition = "TEXT")
    private String value;
    private UCEMetadataValueType valueType;
    @Column(columnDefinition = "TEXT")
    private String comment;

    // TODO: Refactor this json rendering somewhere else, this is disgusting.
    public List<UCEMetadataJsonViewModel> getJsonValueAsIterable() {
        if (this.valueType != UCEMetadataValueType.JSON) return null;
        var jsonString = this.value;
        if (jsonString == null || jsonString.isBlank()) return null;

        var parsed = parseJsonToViewModel(jsonString);
        return parsed;
    }

    private List<UCEMetadataJsonViewModel> parseJsonToViewModel(String jsonString) {
        var jsonElement = JsonParser.parseString(jsonString);
        return parseElement(null, jsonElement);
    }

    private List<UCEMetadataJsonViewModel> parseElement(String parentKey, JsonElement element) {
        List<UCEMetadataJsonViewModel> models = new ArrayList<>();

        if (element.isJsonObject()) {
            // Handle JSON object
            var jsonObject = element.getAsJsonObject();
            for (var entry : jsonObject.entrySet()) {
                var childModel = new UCEMetadataJsonViewModel();
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
                var childModel = new UCEMetadataJsonViewModel();
                childModel.setKey((parentKey != null ? parentKey : "array") + "[" + index + "]");
                childModel.setValueType("array");
                childModel.setChildren(parseElement(null, item)); // Recursively parse children
                models.add(childModel);
                index++;
            }
        } else if (element.isJsonPrimitive()) {
            // Handle primitive values directly as a leaf node
            var primitive = element.getAsJsonPrimitive();
            var leafModel = new UCEMetadataJsonViewModel();
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
            var nullModel = new UCEMetadataJsonViewModel();
            nullModel.setKey(parentKey); // Directly assign the parent key
            nullModel.setValueType("null");
            nullModel.setValue(null);
            models.add(nullModel); // Add the null model directly without nesting
        }

        return models;
    }

    public long getDocumentId() {
        return this.documentId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public UCEMetadataValueType getValueType() {
        return valueType;
    }

    public void setValueType(UCEMetadataValueType valueType) {
        this.valueType = valueType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
