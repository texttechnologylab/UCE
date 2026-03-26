package org.texttechnologylab.uce.common.models.search.promode;

public class ProBinaryNode implements ProQueryExpression {
    private final ProBinaryOperator operator;
    private final int followDistance;
    private final ProQueryExpression left;
    private final ProQueryExpression right;
    private final SourceSpan span;

    public ProBinaryNode(ProBinaryOperator operator,
                         int followDistance,
                         ProQueryExpression left,
                         ProQueryExpression right,
                         SourceSpan span) {
        this.operator = operator;
        this.followDistance = followDistance;
        this.left = left;
        this.right = right;
        this.span = span;
    }

    public ProBinaryOperator operator() {
        return operator;
    }

    public int followDistance() {
        return followDistance;
    }

    public ProQueryExpression left() {
        return left;
    }

    public ProQueryExpression right() {
        return right;
    }

    @Override
    public SourceSpan span() {
        return span;
    }
}
