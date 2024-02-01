package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

public class Time extends UIMAAnnotation {

    private String value;
    private String coveredText;
    public Time(int begin, int end) {
        super(begin, end);
    }

    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
    }
    public String getCoveredText() {
        return coveredText;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}
