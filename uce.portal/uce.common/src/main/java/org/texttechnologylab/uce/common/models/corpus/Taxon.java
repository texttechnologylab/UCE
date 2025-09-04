package org.texttechnologylab.uce.common.models.corpus;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.annotations.Taxonsystem;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@MappedSuperclass
@Typesystem(types = {org.texttechnologylab.annotation.type.Taxon.class})
@Taxonsystem(types = {"gnfindertaxon", "gazetteertaxon"})
public abstract class Taxon extends UIMAAnnotation implements WikiModel{
    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String identifier;

    @Setter
    @Getter
    @Column(name = "\"valuee\"", columnDefinition = "TEXT")
    private String value;

    @Getter
    @Setter
    @Column(name = "recordId")
    /**
     * The record id of this entity which can also be used on gbif. like: https://www.gbif.org/species/6093134
     * and as the BIOfid Url
     */
    private long recordId;

    //@OneToMany(mappedBy = "gbifTaxonId", cascade = CascadeType.ALL)
    //private List<GbifOccurrence> gbifOccurrences;

    public Taxon() {
        super(-1, -1);
    }

    public Taxon(int begin, int end) {
        super(begin, end);
    }

    public List<String> getIdentifierAsList() {
        if (this.getIdentifier() == null || this.getIdentifier().isEmpty()) return new ArrayList<>();
        // Split by | or SPACE
        return Arrays.stream(this.getIdentifier().split("[|\\s]+")).toList();
    }

    @Override
    public abstract String getWikiId();
}
