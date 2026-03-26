package org.texttechnologylab.uce.common.models.search.promode;

public enum ProQueryTokenType {
    TERM,
    QUOTED,
    AND,
    OR,
    NOT,
    LPAREN,
    RPAREN,
    FOLLOWED_BY,
    EOF
}
