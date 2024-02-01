package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

public class Taxon extends UIMAAnnotation {

    private String value;
    private String coveredText;

    public Taxon(int begin, int end) {
        super(begin, end);
    }

    public String getCoveredText() {
        return coveredText;
    }
    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
    }

    public void setValue(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
