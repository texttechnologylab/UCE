package org.texttechnologylab.uce.common.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsConfig {
    private Integer port = null;
    private RAGConfig rag;
    private AnalysisConfig analysis;
    private EmbeddingsConfig embeddings;
    private AuthConfig authentication;
    private MCPConfig mcp = new MCPConfig();
}
