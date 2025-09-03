package org.texttechnologylab.uce.common.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

/**
 * Example config for a Ollama LLM:
 *  {
 *    "model": "ollama/gemma3:4b",
 *    "url": "http://example.com:12441",
 *    "apiKey": "",
 *    "displayName": "Gemma 3 (4b)",
 *    "streaming": true
 *  },
 */
@Getter
@Setter
public class RAGModelConfig {
    private String model;
    private String displayName;
    private String apiKey;
    private String url;
    private boolean streaming = false;
}
