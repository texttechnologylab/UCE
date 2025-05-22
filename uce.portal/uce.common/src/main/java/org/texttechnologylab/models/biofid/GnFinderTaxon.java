package org.texttechnologylab.models.biofid;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Taxon;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "gnfindertaxon")
@Typesystem(types = {
        org.texttechnologylab.annotation.biofid.gnfinder.VerifiedTaxon.class,
        org.texttechnologylab.annotation.biofid.gnfinder.Taxon.class
})
public class GnFinderTaxon extends Taxon implements WikiModel {

    @Getter
    @Setter
    private double oddsLog10;

    @Column(columnDefinition = "TEXT")
    @Getter
    @Setter
    private String matchedName;

    @Column(columnDefinition = "TEXT")
    @Getter
    @Setter
    private String matchedCanonical;

    @Override
    public String getWikiId() { return "TA_GN-" + this.getId(); }

    public GnFinderTaxon(){}
    public GnFinderTaxon(int begin, int end){
        super(begin, end);
    }


}
