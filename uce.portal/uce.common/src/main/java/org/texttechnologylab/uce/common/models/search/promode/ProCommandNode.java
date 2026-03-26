package org.texttechnologylab.uce.common.models.search.promode;

public class ProCommandNode extends ProQueryOperand {
    private final String command;
    private final String value;

    public ProCommandNode(String command, String value, SourceSpan span) {
        super(span, value);
        this.command = command;
        this.value = value;
    }

    public String command() {
        return command;
    }

    public String value() {
        return value;
    }
}
