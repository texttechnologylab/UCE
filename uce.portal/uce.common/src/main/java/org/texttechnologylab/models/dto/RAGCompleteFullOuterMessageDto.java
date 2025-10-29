package org.texttechnologylab.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class RAGCompleteFullOuterMessageDto {
    @Setter
    @Getter
    private RAGCompleteFullMessageDto message;

    public RAGCompleteFullOuterMessageDto() {
    }

}
