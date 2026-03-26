package org.texttechnologylab.uce.common.models.search.promode;

public class ProGroupNode implements ProQueryExpression {
    private final ProQueryExpression inner;
    private final SourceSpan span;

    public ProGroupNode(ProQueryExpression inner, SourceSpan span) {
        this.inner = inner;
        this.span = span;
    }

    public ProQueryExpression inner() {
        return inner;
    }

    @Override
    public SourceSpan span() {
        return span;
    }
}
