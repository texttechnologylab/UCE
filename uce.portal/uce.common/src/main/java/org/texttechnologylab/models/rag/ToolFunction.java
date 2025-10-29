package org.texttechnologylab.models.rag;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import lombok.Setter;

public class ToolFunction {
    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private String description;

    @Setter
    @Getter
    private McpSchema.JsonSchema parameters;

    public ToolFunction(String name, String description, McpSchema.JsonSchema parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }
}
