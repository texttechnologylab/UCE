package org.texttechnologylab.uce.common.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RAGConfig {
    private List<RAGModelConfig> models;
}
