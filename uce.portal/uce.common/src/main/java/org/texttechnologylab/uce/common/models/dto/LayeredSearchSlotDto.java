package org.texttechnologylab.uce.common.models.dto;

public class LayeredSearchSlotDto {

    private String value;
    private LayeredSearchSlotType type;

    public LayeredSearchSlotDto(){}

    public String getValue() {
        return value;
    }

    public String getCleanedValue(){return getValue().replace("'", "\\'");}

    public void setValue(String value) {
        this.value = value;
    }

    public LayeredSearchSlotType getType() {
        return type;
    }

    public void setType(LayeredSearchSlotType type) {
        this.type = type;
    }
}
