package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.gbif.GbifOccurrence;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name="taxon")
public class Taxon extends UIMAAnnotation implements WikiModel {
    @Override
    public String getWikiId() {
        return "TA" + "-" + this.getId();
    }

    @Column(name = "document_id", nullable = false, insertable = true, updatable = true)
    private Long documentId;

    @Column(name = "\"valuee\"", columnDefinition = "TEXT")
    private String value;

    @Column(name = "value_array")
    private String valueArray;

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

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
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
    public List<String> getIdentifierAsList(){
        if(this.getIdentifier() == null || this.getIdentifier().isEmpty()) return new ArrayList<>();
        // Split by | or SPACE
        return Arrays.stream(this.getIdentifier().split("[|\\s]+")).toList();
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
