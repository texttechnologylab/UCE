package org.texttechnologylab.uce.common.models.search.promode;

public class ProTermNode extends ProQueryOperand {
    private final String value;
    private final boolean quoted;
    private final boolean prefixSearch;

    public ProTermNode(String value, boolean quoted, boolean prefixSearch, SourceSpan span) {
        super(span, value);
        this.value = value;
        this.quoted = quoted;
        this.prefixSearch = prefixSearch;
    }

    public String value() {
        return value;
    }

    public boolean quoted() {
        return quoted;
    }

    public boolean prefixSearch() {
        return prefixSearch;
    }
}
