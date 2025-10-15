package org.texttechnologylab.uce.common.models.biofid;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.WikiModel;
import org.texttechnologylab.uce.common.models.corpus.Taxon;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "gazetteertaxon")
@Typesystem(types = {
        org.texttechnologylab.annotation.type.Taxon.class,
        org.texttechnologylab.annotation.biofid.gazetteer.Taxon.class
})
public class GazetteerTaxon extends Taxon implements WikiModel {

    @Getter
    @Setter
    private String primaryIdentifier;

    public GazetteerTaxon(){}

    public GazetteerTaxon(int begin, int end) {
        super(begin, end);
    }

    @Override
    public String getWikiId() {
        return "TA_GA-" + this.getId();
    }
}
