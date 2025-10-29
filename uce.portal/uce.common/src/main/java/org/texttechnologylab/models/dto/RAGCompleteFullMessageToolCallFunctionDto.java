package org.texttechnologylab.models.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public class RAGCompleteFullMessageToolCallFunctionDto {
    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private Map<String, Object> arguments;

    public RAGCompleteFullMessageToolCallFunctionDto() {
    }

}
