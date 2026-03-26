package org.texttechnologylab.uce.common.models.search.promode;

import org.texttechnologylab.uce.common.models.search.EnrichedSearchToken;
import org.texttechnologylab.uce.common.models.search.EnrichedSearchTokenType;

import java.util.ArrayList;
import java.util.List;

public class EnrichedTokenViewAdapter {

    public List<EnrichedSearchToken> fromAst(ProQueryAst ast) {
        var out = new ArrayList<EnrichedSearchToken>();
        walk(ast.root(), out);
        return out.stream().filter(t -> t.getValue() != null && !t.getValue().trim().isBlank()).toList();
    }

    private void walk(ProQueryExpression expression, List<EnrichedSearchToken> out) {
        if (expression instanceof ProBinaryNode b) {
            walk(b.left(), out);
            String op = switch (b.operator()) {
                case AND -> "&";
                case OR -> "|";
                case FOLLOWED_BY -> b.followDistance() <= 1 ? "<->" : "<" + b.followDistance() + ">";
            };
            out.add(new EnrichedSearchToken(op, EnrichedSearchTokenType.OPERATOR));
            walk(b.right(), out);
            return;
        }
        if (expression instanceof ProUnaryNode u) {
            out.add(new EnrichedSearchToken("!", EnrichedSearchTokenType.OPERATOR));
            walk(u.operand(), out);
            return;
        }
        if (expression instanceof ProGroupNode g) {
            out.add(new EnrichedSearchToken("(", EnrichedSearchTokenType.OPERATOR));
            walk(g.inner(), out);
            out.add(new EnrichedSearchToken(")", EnrichedSearchTokenType.OPERATOR));
            return;
        }
        if (expression instanceof ProQueryOperand o) {
            var token = new EnrichedSearchToken(o.enrichment().getOriginal(), o.enrichment().getTokenType());
            var expanded = o.enrichment().getExpandedValues();
            if (expanded != null && !expanded.isEmpty()) {
                EnrichedSearchTokenType childType = switch (o.enrichment().getTokenType()) {
                    case LOCATION_COMMAND -> EnrichedSearchTokenType.LOCATION;
                    case TIME_COMMAND -> EnrichedSearchTokenType.TIME;
                    default -> EnrichedSearchTokenType.TAXON;
                };
                token.setChildren(expanded.stream().map(v -> new EnrichedSearchToken(v, childType)).toList());
            }
            if (o.enrichment().getGroupedChildren() != null && !o.enrichment().getGroupedChildren().isEmpty()) {
                token.setGroupedChildren(o.enrichment().getGroupedChildren());
            }
            out.add(token);
        }
    }
}
