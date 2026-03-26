package org.texttechnologylab.uce.common.models.search.promode;

public abstract class ProQueryOperand implements ProQueryExpression {
    private final SourceSpan span;
    private final EnrichmentBundle enrichment;

    protected ProQueryOperand(SourceSpan span, String originalValue) {
        this.span = span;
        this.enrichment = new EnrichmentBundle(originalValue);
    }

    @Override
    public SourceSpan span() {
        return span;
    }

    public EnrichmentBundle enrichment() {
        return enrichment;
    }
}
