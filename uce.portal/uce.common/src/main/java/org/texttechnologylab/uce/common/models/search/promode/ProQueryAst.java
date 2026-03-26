package org.texttechnologylab.uce.common.models.search.promode;

public class ProQueryAst {
    private final ProQueryExpression root;

    public ProQueryAst(ProQueryExpression root) {
        this.root = root;
    }

    public ProQueryExpression root() {
        return root;
    }
}
