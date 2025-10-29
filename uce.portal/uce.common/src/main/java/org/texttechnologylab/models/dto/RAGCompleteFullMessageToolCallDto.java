package org.texttechnologylab.models.dto;

import lombok.Getter;
import lombok.Setter;

public class RAGCompleteFullMessageToolCallDto {
    @Setter
    @Getter
    private RAGCompleteFullMessageToolCallFunctionDto function;

    public RAGCompleteFullMessageToolCallDto() {
    }

}
