package org.texttechnologylab.uce.common.models.dto.rdf;

import com.google.gson.annotations.SerializedName;

public class RDFAskDto extends RDFRequestDto {

    private HeadDto head;

    @SerializedName("boolean")
    private boolean bool;

    public HeadDto getHead() {
        return head;
    }

    public void setHead(HeadDto head) {
        this.head = head;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }
}
