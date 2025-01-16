package org.texttechnologylab.models.dto;

import org.texttechnologylab.models.corpus.UCEMetadataValueType;

public class UCEMetadataFilterDto {
    private String key;
    private UCEMetadataValueType valueType;
    private String value;

    public UCEMetadataFilterDto(){}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public UCEMetadataValueType getValueType() {
        return valueType;
    }

    public void setValueType(UCEMetadataValueType valueType) {
        this.valueType = valueType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
