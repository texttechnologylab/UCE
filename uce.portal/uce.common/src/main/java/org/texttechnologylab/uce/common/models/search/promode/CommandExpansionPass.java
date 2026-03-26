package org.texttechnologylab.uce.common.models.search.promode;

public class CommandExpansionPass {

    public void apply(ProQueryAst ast, ProExpansionResolver resolver) throws Exception {
        visit(ast.root(), resolver);
    }

    private void visit(ProQueryExpression expression, ProExpansionResolver resolver) throws Exception {
        if (expression instanceof ProBinaryNode b) {
            visit(b.left(), resolver);
            visit(b.right(), resolver);
            return;
        }
        if (expression instanceof ProUnaryNode u) {
            visit(u.operand(), resolver);
            return;
        }
        if (expression instanceof ProGroupNode g) {
            visit(g.inner(), resolver);
            return;
        }
        if (expression instanceof ProCommandNode c) {
            var res = resolver.resolveCommand(c.command(), c.value());
            if (res == null) return;
            c.enrichment().setTokenType(res.tokenType());
            if (res.flatValues() != null) c.enrichment().getExpandedValues().addAll(res.flatValues());
            c.enrichment().setGroupedChildren(res.groupedChildren());
        }
    }
}
