package org.texttechnologylab.uce.common.models.biofid;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.WikiModel;
import org.texttechnologylab.uce.common.models.corpus.Taxon;

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

    @Getter
    @Setter
    private boolean isVerified;

    @Override
    public String getWikiId() { return "TA_GN-" + this.getId(); }

    public GnFinderTaxon(){}
    public GnFinderTaxon(int begin, int end){
        super(begin, end);
    }


}
