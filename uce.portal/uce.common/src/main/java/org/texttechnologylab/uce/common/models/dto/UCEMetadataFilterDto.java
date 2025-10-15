package org.texttechnologylab.uce.common.models.dto;

import org.texttechnologylab.uce.common.models.corpus.UCEMetadataValueType;

public class UCEMetadataFilterDto {
    private String key;
    private UCEMetadataValueType valueType;
    private String value;
    private Float min;
    private Float max;

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

    public Float getMin() {
        return min;
    }

    public void setMin(Float min) {
        this.min = min;
    }

    public Float getMax() {
        return max;
    }

    public void setMax(Float max) {
        this.max = max;
    }
}
