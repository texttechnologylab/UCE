package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

public class NamedEntity extends UIMAAnnotation {

    private String type;
    private String lemmaValue;

    public NamedEntity(int begin, int end) {
        super(begin, end);
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getLemmaValue() {
        return lemmaValue;
    }
    public void setLemmaValue(String lemmaValue) {
        this.lemmaValue = lemmaValue;
    }
}
