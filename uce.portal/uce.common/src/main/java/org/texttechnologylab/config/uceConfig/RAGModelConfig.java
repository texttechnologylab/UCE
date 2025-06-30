package org.texttechnologylab.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RAGModelConfig {
    private String model;
    private String displayName;
    private String apiKey;
    private String url;
}
