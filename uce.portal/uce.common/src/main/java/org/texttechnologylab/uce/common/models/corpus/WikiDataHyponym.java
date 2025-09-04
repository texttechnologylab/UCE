package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "wikiDataHyponym")
@Typesystem(types = {org.hucompute.textimager.uima.type.wikidata.WikiDataHyponym.class})
public class WikiDataHyponym extends ModelBase {

    @Column(name = "\"valuee\"")
    private String value;

    public WikiDataHyponym() {
    }

    public WikiDataHyponym(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
