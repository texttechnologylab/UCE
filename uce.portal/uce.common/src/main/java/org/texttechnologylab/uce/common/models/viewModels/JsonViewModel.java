package org.texttechnologylab.uce.common.models.viewModels;

import java.util.ArrayList;
import java.util.List;

public class JsonViewModel {

    private String key;
    private String value;
    private String valueType;
    private List<JsonViewModel> children;

    public JsonViewModel() {
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

    public List<JsonViewModel> getChildren() {
        return children;
    }

    public void setChildren(List<JsonViewModel> children) {
        this.children = children;
    }
}
