package org.texttechnologylab.uce.common.models.search.promode;

public class ProUnaryNode implements ProQueryExpression {
    private final ProQueryExpression operand;
    private final SourceSpan span;

    public ProUnaryNode(ProQueryExpression operand, SourceSpan span) {
        this.operand = operand;
        this.span = span;
    }

    public ProQueryExpression operand() {
        return operand;
    }

    @Override
    public SourceSpan span() {
        return span;
    }
}
