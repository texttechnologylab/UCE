package org.texttechnologylab.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class RAGCompleteFullMessageDto {
    @Setter
    @Getter
    private String content;

    @Setter
    @Getter
    @JsonProperty("tool_calls")
    @SerializedName("tool_calls")
    private List<RAGCompleteFullMessageToolCallDto> toolCalls;

    public RAGCompleteFullMessageDto() {
    }

}
