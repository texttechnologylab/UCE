package org.texttechnologylab.uce.common.models.search.promode;

public record ProQueryToken(ProQueryTokenType type, String text, int followDistance, SourceSpan span) {
    public static ProQueryToken of(ProQueryTokenType type, String text, int startInclusive, int endExclusive) {
        return new ProQueryToken(type, text, 0, SourceSpan.of(startInclusive, endExclusive));
    }

    public static ProQueryToken followedBy(String text, int distance, int startInclusive, int endExclusive) {
        return new ProQueryToken(ProQueryTokenType.FOLLOWED_BY, text, distance, SourceSpan.of(startInclusive, endExclusive));
    }
}
