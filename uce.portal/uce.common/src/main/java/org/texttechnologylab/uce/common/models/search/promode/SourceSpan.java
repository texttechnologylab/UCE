package org.texttechnologylab.uce.common.models.search.promode;

public record SourceSpan(int startInclusive, int endExclusive) {
    public static SourceSpan of(int startInclusive, int endExclusive) {
        return new SourceSpan(Math.max(0, startInclusive), Math.max(startInclusive, endExclusive));
    }
}
