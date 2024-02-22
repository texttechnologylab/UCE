package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="namedEntity")
public class NamedEntity extends UIMAAnnotation {

    @Column(name = "\"typee\"")
    private String type;
    private String coveredText;

    public NamedEntity(){
        super(-1, -1);
    }

    public NamedEntity(int begin, int end) {
        super(begin, end);
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getCoveredText() {
        return coveredText;
    }
    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
    }
}
