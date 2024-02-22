package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="time")
public class Time extends UIMAAnnotation {

    @Column(name = "\"valuee\"")
    private String value;
    private String coveredText;
    public Time(){
        super(-1, -1);
    }
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
