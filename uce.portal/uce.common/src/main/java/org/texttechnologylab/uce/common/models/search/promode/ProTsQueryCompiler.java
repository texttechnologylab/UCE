package org.texttechnologylab.uce.common.models.search.promode;

import java.util.List;

public class ProTsQueryCompiler {

    public String compile(ProQueryAst ast) {
        return compileExpression(ast.root(), true).trim();
    }

    private String compileExpression(ProQueryExpression expression, boolean topLevel) {
        if (expression instanceof ProBinaryNode b) {
            String op = switch (b.operator()) {
                case AND -> " & ";
                case OR -> " | ";
                case FOLLOWED_BY -> (b.followDistance() <= 1 ? " <-> " : " <" + b.followDistance() + "> ");
            };
            String left = compileExpression(b.left(), false);
            String right = compileExpression(b.right(), false);
            String result = left + op + right;
            return topLevel ? result : "(" + result + ")";
        }
        if (expression instanceof ProUnaryNode u) {
            return "!" + compileExpression(u.operand(), false);
        }
        if (expression instanceof ProGroupNode g) {
            return "(" + compileExpression(g.inner(), true) + ")";
        }
        if (expression instanceof ProTermNode t) {
            return compileOperand(t.enrichment().getOriginal(), t.prefixSearch(), t.enrichment().getExpandedValues());
        }
        if (expression instanceof ProCommandNode c) {
            return compileOperand(c.value(), false, c.enrichment().getExpandedValues());
        }

        throw new ProModeSyntaxException("Unsupported expression node: " + expression.getClass().getSimpleName());
    }

    private String compileOperand(String original, boolean prefixSearch, List<String> expansions) {
        String normalizedOriginal = sanitizeTerm(original, prefixSearch);
        if (expansions == null || expansions.isEmpty()) return normalizedOriginal;

        StringBuilder sb = new StringBuilder("(");
        sb.append(normalizedOriginal);
        for (var n : expansions) {
            sb.append(" | ").append(quoteTerm(n));
        }
        sb.append(")");
        return sb.toString();
    }

    private String sanitizeTerm(String original, boolean prefixSearch) {
        if (original == null) return "''";
        String raw = original.trim();
        if (raw.isEmpty()) return "''";
        if (prefixSearch) {
            return quoteTerm(raw) + ":*";
        }
        return quoteTerm(raw);
    }

    private String quoteTerm(String value) {
        return "'" + value.replace("'", "") + "'";
    }
}
