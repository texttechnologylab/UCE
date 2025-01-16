package org.texttechnologylab.models.viewModels;

import java.util.ArrayList;
import java.util.List;

public class UCEMetadataJsonViewModel {

    private String key;
    private String value;
    private String valueType;
    private List<UCEMetadataJsonViewModel> children;

    public UCEMetadataJsonViewModel() {
        this.children = new ArrayList<>();
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

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public List<UCEMetadataJsonViewModel> getChildren() {
        return children;
    }

    public void setChildren(List<UCEMetadataJsonViewModel> children) {
        this.children = children;
    }
}
