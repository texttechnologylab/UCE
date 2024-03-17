package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.gbif.GbifOccurrence;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="taxon")
public class Taxon extends UIMAAnnotation {

    @Column(name = "\"valuee\"", columnDefinition = "TEXT")
    private String value;

    @Column(columnDefinition = "TEXT")
    private String identifier;

    @Column(name = "gbiftaxonid")
    /**
     * The taxon id of this entity which can also be used on gbif. like: https://www.gbif.org/species/6093134
     */
    private long gbifTaxonId;

    @OneToMany(mappedBy = "gbifTaxonId", cascade = CascadeType.ALL)
    private List<GbifOccurrence> gbifOccurrences;

    private String primaryBiofidOntologyIdentifier;

    public Taxon(){
        super(-1, -1);
    }

    public Taxon(int begin, int end) {
        super(begin, end);
    }

    public String getPrimaryBiofidOntologyIdentifier() {
        return primaryBiofidOntologyIdentifier;
    }

    public void setPrimaryBiofidOntologyIdentifier(String primaryBiofidOntologyIdentifier) {
        this.primaryBiofidOntologyIdentifier = primaryBiofidOntologyIdentifier;
    }

    public long getGbifTaxonId() {
        return gbifTaxonId;
    }

    public void setGbifTaxonId(long gbifTaxonId) {
        this.gbifTaxonId = gbifTaxonId;
    }

    public List<GbifOccurrence> getGbifOccurrences() {
        return gbifOccurrences;
    }

    public void setGbifOccurrences(List<GbifOccurrence> gbifOccurrences) {
        this.gbifOccurrences = gbifOccurrences;
    }

    public String getIdentifier() {
        return identifier;
    }
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setValue(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
