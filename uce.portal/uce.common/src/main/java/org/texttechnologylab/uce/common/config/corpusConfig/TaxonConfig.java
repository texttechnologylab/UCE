package org.texttechnologylab.uce.common.config.corpusConfig;

public class TaxonConfig{
    private boolean annotated;
    private boolean biofidOnthologyAnnotated;

    public boolean isAnnotated() {
        return annotated;
    }

    public void setAnnotated(boolean annotated) {
        this.annotated = annotated;
    }

    public boolean isBiofidOnthologyAnnotated() {
        return biofidOnthologyAnnotated;
    }

    public void setBiofidOnthologyAnnotated(boolean biofidOnthologyAnnotated) {
        this.biofidOnthologyAnnotated = biofidOnthologyAnnotated;
    }
}
