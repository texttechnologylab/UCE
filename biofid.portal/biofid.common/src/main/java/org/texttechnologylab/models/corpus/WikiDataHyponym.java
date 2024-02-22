package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="wikiDataHyponym")
public class WikiDataHyponym extends ModelBase {

    @Column(name = "\"valuee\"")
    private String value;

    public WikiDataHyponym(){}

    public WikiDataHyponym(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}
