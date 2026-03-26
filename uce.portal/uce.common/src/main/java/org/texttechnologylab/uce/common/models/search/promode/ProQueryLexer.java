package org.texttechnologylab.uce.common.models.search.promode;

import java.util.ArrayList;
import java.util.List;

public class ProQueryLexer {

    public List<ProQueryToken> tokenize(String input) {
        var tokens = new ArrayList<ProQueryToken>();
        if (input == null) input = "";
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            int start = i;

            if (c == '(') {
                tokens.add(ProQueryToken.of(ProQueryTokenType.LPAREN, "(", start, ++i));
                continue;
            }
            if (c == ')') {
                tokens.add(ProQueryToken.of(ProQueryTokenType.RPAREN, ")", start, ++i));
                continue;
            }
            if (c == '&') {
                tokens.add(ProQueryToken.of(ProQueryTokenType.AND, "&", start, ++i));
                continue;
            }
            if (c == '|') {
                tokens.add(ProQueryToken.of(ProQueryTokenType.OR, "|", start, ++i));
                continue;
            }
            if (c == '!') {
                tokens.add(ProQueryToken.of(ProQueryTokenType.NOT, "!", start, ++i));
                continue;
            }
            if (startsWith(input, i, "<->")) {
                tokens.add(ProQueryToken.followedBy("<->", 1, start, i + 3));
                i += 3;
                continue;
            }
            if (c == '<') {
                int end = input.indexOf('>', i + 1);
                if (end > i + 1) {
                    String between = input.substring(i + 1, end).trim();
                    if (between.matches("\\d+")) {
                        int distance = Integer.parseInt(between);
                        if (distance <= 0) {
                            throw new ProModeSyntaxException("Invalid <N> distance at position " + i + ": must be > 0");
                        }
                        tokens.add(ProQueryToken.followedBy("<" + between + ">", distance, start, end + 1));
                        i = end + 1;
                        continue;
                    }
                }
                throw new ProModeSyntaxException("Invalid followed-by operator at position " + i + ". Use <-> or <N>.");
            }
            if (c == '\'' || c == '"') {
                char quote = c;
                i++;
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (i < input.length()) {
                    char qc = input.charAt(i);
                    if (qc == '\\' && i + 1 < input.length()) {
                        sb.append(input.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    if (qc == quote) {
                        closed = true;
                        i++;
                        break;
                    }
                    sb.append(qc);
                    i++;
                }
                if (!closed) {
                    throw new ProModeSyntaxException("Unclosed quote starting at position " + start);
                }
                tokens.add(ProQueryToken.of(ProQueryTokenType.QUOTED, sb.toString(), start, i));
                continue;
            }

            while (i < input.length()) {
                char cc = input.charAt(i);
                if (Character.isWhitespace(cc) || cc == '(' || cc == ')' || cc == '&' || cc == '|' || cc == '!') break;
                if (cc == '<') break;
                i++;
            }
            String text = input.substring(start, i);
            if (!text.isBlank()) tokens.add(ProQueryToken.of(ProQueryTokenType.TERM, text, start, i));
        }

        tokens.add(ProQueryToken.of(ProQueryTokenType.EOF, "", input.length(), input.length()));
        return tokens;
    }

    private boolean startsWith(String s, int idx, String prefix) {
        return s.regionMatches(idx, prefix, 0, prefix.length());
    }
}
