package org.texttechnologylab.models.dto;

import lombok.Getter;
import lombok.Setter;

public class RAGCompleteFullDto {
    @Setter
    @Getter
    private RAGCompleteFullOuterMessageDto message;

    @Setter
    @Getter
    private int status;

    public RAGCompleteFullDto(){}
}
