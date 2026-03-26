package org.texttechnologylab.uce.common.models.search.promode;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class NormalizationPass {

    public void apply(ProQueryAst ast) {
        visit(ast.root());
    }

    private void visit(ProQueryExpression expression) {
        if (expression instanceof ProBinaryNode b) {
            visit(b.left());
            visit(b.right());
            return;
        }
        if (expression instanceof ProUnaryNode u) {
            visit(u.operand());
            return;
        }
        if (expression instanceof ProGroupNode g) {
            visit(g.inner());
            return;
        }
        if (expression instanceof ProQueryOperand o) {
            var distinct = new LinkedHashSet<String>();
            for (var v : o.enrichment().getExpandedValues()) {
                if (v == null) continue;
                var trimmed = v.trim();
                if (trimmed.isEmpty()) continue;
                distinct.add(trimmed);
            }
            o.enrichment().getExpandedValues().clear();
            o.enrichment().getExpandedValues().addAll(new ArrayList<>(distinct));
        }
    }
}
