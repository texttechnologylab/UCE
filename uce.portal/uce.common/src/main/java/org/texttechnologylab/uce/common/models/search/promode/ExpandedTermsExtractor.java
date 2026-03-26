package org.texttechnologylab.uce.common.models.search.promode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class ExpandedTermsExtractor {

    public List<String> extract(ProQueryAst ast) {
        var out = new LinkedHashSet<String>();
        collect(ast.root(), out);
        return new ArrayList<>(out);
    }

    private void collect(ProQueryExpression expression, LinkedHashSet<String> out) {
        if (expression instanceof ProBinaryNode b) {
            collect(b.left(), out);
            collect(b.right(), out);
            return;
        }
        if (expression instanceof ProUnaryNode u) {
            collect(u.operand(), out);
            return;
        }
        if (expression instanceof ProGroupNode g) {
            collect(g.inner(), out);
            return;
        }
        if (expression instanceof ProQueryOperand o) {
            out.addAll(o.enrichment().getExpandedValues());
        }
    }
}
