package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="time")
public class Time extends UIMAAnnotation {
    @Column(name = "\"valuee\"", columnDefinition = "TEXT")
    private String value;
    public Time(){
        super(-1, -1);
    }
    public Time(int begin, int end) {
        super(begin, end);
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}