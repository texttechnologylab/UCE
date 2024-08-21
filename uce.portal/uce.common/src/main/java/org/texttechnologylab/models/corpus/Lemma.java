package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="lemma")
public class Lemma extends UIMAAnnotation {

    private String value;

    public Lemma(int begin, int end) {
        super(begin, end);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
