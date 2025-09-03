package org.texttechnologylab.uce.common.models.dto;

import lombok.Getter;
import lombok.Setter;

public class RAGCompleteMessageStreamingDto {
    // TODO map this to the UCE role class
    @Setter
    @Getter
    private String role;

    @Setter
    @Getter
    private String content;

    // TODO would also contain "images", left out for now as we do not support receiving images

    public RAGCompleteMessageStreamingDto() {
    }
}
