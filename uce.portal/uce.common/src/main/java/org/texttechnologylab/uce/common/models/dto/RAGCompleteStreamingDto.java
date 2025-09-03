package org.texttechnologylab.uce.common.models.dto;

import lombok.Getter;
import lombok.Setter;

public class RAGCompleteStreamingDto {
    @Setter
    @Getter
    private RAGCompleteMessageStreamingDto message;

    @Setter
    @Getter
    private boolean done;

    public RAGCompleteStreamingDto() {
    }
}
