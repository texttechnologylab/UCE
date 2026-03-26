package org.texttechnologylab.uce.common.models.search.promode;

import java.util.List;
import java.util.Set;

public class ProQueryParser {

    private static final Set<String> COMMAND_PREFIXES = Set.of(
            "K::", "P::", "C::", "O::", "F::", "G::", "S::",
            "LOC::", "R::",
            "Y::", "M::", "D::", "E::", "T::"
    );

    private List<ProQueryToken> tokens;
    private int pos;

    public ProQueryAst parse(String query) {
        this.tokens = new ProQueryLexer().tokenize(query);
        this.pos = 0;
        ProQueryExpression expr = parseOr();
        ProQueryToken current = current();
        if (current.type() != ProQueryTokenType.EOF) {
            throw syntax("Unexpected token '" + current.text() + "'", current);
        }
        return new ProQueryAst(expr);
    }

    private ProQueryExpression parseOr() {
        ProQueryExpression left = parseAnd();
        while (match(ProQueryTokenType.OR)) {
            ProQueryToken op = previous();
            ProQueryExpression right = parseAnd();
            left = new ProBinaryNode(ProBinaryOperator.OR, 0, left, right, SourceSpan.of(left.span().startInclusive(), right.span().endExclusive()));
        }
        return left;
    }

    private ProQueryExpression parseAnd() {
        ProQueryExpression left = parseFollowedBy();
        while (match(ProQueryTokenType.AND)) {
            ProQueryToken op = previous();
            ProQueryExpression right = parseFollowedBy();
            left = new ProBinaryNode(ProBinaryOperator.AND, 0, left, right, SourceSpan.of(left.span().startInclusive(), right.span().endExclusive()));
        }
        return left;
    }

    private ProQueryExpression parseFollowedBy() {
        ProQueryExpression left = parseUnary();
        while (match(ProQueryTokenType.FOLLOWED_BY)) {
            ProQueryToken op = previous();
            ProQueryExpression right = parseUnary();
            left = new ProBinaryNode(ProBinaryOperator.FOLLOWED_BY, op.followDistance(), left, right,
                    SourceSpan.of(left.span().startInclusive(), right.span().endExclusive()));
        }
        return left;
    }

    private ProQueryExpression parseUnary() {
        if (match(ProQueryTokenType.NOT)) {
            ProQueryToken op = previous();
            ProQueryExpression operand = parseUnary();
            return new ProUnaryNode(operand, SourceSpan.of(op.span().startInclusive(), operand.span().endExclusive()));
        }
        return parsePrimary();
    }

    private ProQueryExpression parsePrimary() {
        if (match(ProQueryTokenType.LPAREN)) {
            ProQueryToken left = previous();
            ProQueryExpression inner = parseOr();
            ProQueryToken right = consume(ProQueryTokenType.RPAREN, "Expected ')' to close group");
            return new ProGroupNode(inner, SourceSpan.of(left.span().startInclusive(), right.span().endExclusive()));
        }

        if (match(ProQueryTokenType.QUOTED)) {
            ProQueryToken token = previous();
            return new ProTermNode(token.text(), true, false, token.span());
        }

        if (match(ProQueryTokenType.TERM)) {
            ProQueryToken token = previous();
            String text = token.text();
            String commandPrefix = commandPrefix(text);
            if (commandPrefix != null) {
                String command = commandPrefix;
                String value = text.length() > command.length() ? text.substring(command.length()) : "";
                if (value.isBlank()) {
                    throw syntax("Command '" + command + "' requires a value", token);
                }
                validateCommandValue(command, value, token);
                return new ProCommandNode(command, value, token.span());
            }

            boolean prefixSearch = text.endsWith(":*");
            if (prefixSearch && text.length() <= 2) {
                throw syntax("Prefix search ':*' requires a term", token);
            }
            String normalized = prefixSearch ? text.substring(0, text.length() - 2) : text;
            if (normalized.isBlank()) {
                throw syntax("Empty term is not allowed", token);
            }
            return new ProTermNode(normalized, false, prefixSearch, token.span());
        }

        throw syntax("Expected a term, phrase, command or group", current());
    }

    private boolean looksLikeCommand(String text) {
        return commandPrefix(text) != null;
    }

    private String commandPrefix(String text) {
        if (text == null || text.isBlank()) return null;
        return COMMAND_PREFIXES.stream()
                .filter(text::startsWith)
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .findFirst()
                .orElse(null);
    }

    private void validateCommandValue(String command, String value, ProQueryToken token) {
        if ("R::".equals(command)) {
            if (!(value.contains("lng=") && value.contains("lat=") && value.contains("r="))) {
                throw syntax("R:: command must use format lng=<LONGITUDE>;lat=<LATITUDE>;r=<RADIUS>", token);
            }
        }
        if ("T::".equals(command)) {
            if (!value.matches("\\d{1,4}\\s*-\\s*\\d{1,4}")) {
                throw syntax("T:: command must use format <year>-<year>", token);
            }
        }
    }

    private ProModeSyntaxException syntax(String message, ProQueryToken token) {
        return new ProModeSyntaxException(message + " at position " + token.span().startInclusive());
    }

    private boolean match(ProQueryTokenType type) {
        if (check(type)) {
            pos++;
            return true;
        }
        return false;
    }

    private ProQueryToken consume(ProQueryTokenType type, String message) {
        if (check(type)) return tokens.get(pos++);
        throw syntax(message, current());
    }

    private boolean check(ProQueryTokenType type) {
        return current().type() == type;
    }

    private ProQueryToken current() {
        return tokens.get(Math.min(pos, tokens.size() - 1));
    }

    private ProQueryToken previous() {
        return tokens.get(Math.max(0, pos - 1));
    }
}
