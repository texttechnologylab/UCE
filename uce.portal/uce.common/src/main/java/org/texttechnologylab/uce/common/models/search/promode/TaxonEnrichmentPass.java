package org.texttechnologylab.uce.common.models.search.promode;

public class TaxonEnrichmentPass {

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
        if (expression instanceof ProTermNode t) {
            if (t.prefixSearch()) return;
            var res = resolver.resolveTaxonTerm(t.value());
            if (res == null) return;
            t.enrichment().setTokenType(res.tokenType());
            if (res.flatValues() != null) t.enrichment().getExpandedValues().addAll(res.flatValues());
            t.enrichment().setGroupedChildren(res.groupedChildren());
        }
    }
}
