package org.texttechnologylab.uce.common.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class EmbeddingsConfig {
    /*
        Example config:

        "settings": {
            "embeddings": {
                "backend": "ollama",
                "timeout": 120,
                "parameters": {
                    "base_url": "http://localhost:11434",
                    "model": "llama3.2:latest"
                }
            }
        }
     */

    // Backend for embeddings, e.g. Ollama server or SentenceTransformers
    private String backend;

    // Additional parameters for the embeddings backend, such as model name or API key
    private Map<String, Object> parameters;

    // Request timeout in seconds
    private long timeout = 200;

}
