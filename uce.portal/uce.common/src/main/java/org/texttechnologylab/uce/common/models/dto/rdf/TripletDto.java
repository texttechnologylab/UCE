package org.texttechnologylab.uce.common.models.dto.rdf;

public class TripletDto {

    private String type;
    private String value;
    private String datatype;

    public TripletDto(){

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }
}
