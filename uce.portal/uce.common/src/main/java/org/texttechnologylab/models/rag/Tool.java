package org.texttechnologylab.models.rag;

import lombok.Getter;

public class Tool {
    @Getter
    private String type;

    @Getter
    private ToolFunction function;

    public Tool(ToolFunction function) {
        // TODO for now, we only support functions...
        // TODO can we automate this more?
        this.type = "function";
        this.function = function;
    }
}
