package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="taxon")
public class Taxon extends UIMAAnnotation {

    @Column(name = "\"valuee\"")
    private String value;
    private String coveredText;

    public Taxon(){
        super(-1, -1);
    }

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
