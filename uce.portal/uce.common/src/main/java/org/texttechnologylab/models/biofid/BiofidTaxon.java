package org.texttechnologylab.models.biofid;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.corpus.Page;
import org.texttechnologylab.models.corpus.Taxon;
import org.texttechnologylab.models.dto.rdf.RDFNodeDto;

import javax.persistence.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "biofidtaxon")
@Typesystem(types = {org.texttechnologylab.annotation.type.Taxon.class})
public class BiofidTaxon extends UIMAAnnotation implements Cloneable {

    /*public String getWikiId() {
        return "TA-" + this.getId();
    }*/

    @Override
    public BiofidTaxon clone() {
        try {
            return (BiofidTaxon) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Cloning of BiofidTaxon failed", ex);
        }
    }

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Getter
    @Setter
    @Column(name = "document_id", insertable = false, updatable = false)
    private Long documentId;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @Getter
    @Setter
    @Column(name = "page_id", insertable = false, updatable = false)
    private Long pageId;

    @Getter
    @Setter
    private String biofidUrl;

    @Getter
    @Setter
    private String originalAnnotatedTaxonTable;

    @Setter
    @Getter
    private String primaryName;

    @Getter
    @Setter
    private String vernacularName;

    @Getter
    @Setter
    private String scientificName;

    @Getter
    @Setter
    private String cleanedScientificName;

    @Getter
    @Setter
    private String kingdom;

    @Getter
    @Setter
    private String phylum;

    @Getter
    @Setter
    private String clazz;

    @Getter
    @Setter
    @Column(name = "orderr")
    private String order;

    @Getter
    @Setter
    private String family;

    @Getter
    @Setter
    private String genus;

    @Getter
    @Setter
    private TaxonRank taxonRank;

    @Getter
    @Setter
    private String author;

    @Getter
    @Setter
    private boolean isVernacular;

    public BiofidTaxon(int begin, int end) {
        super(begin, end);
    }

    public BiofidTaxon() {
    }

    public static List<BiofidTaxon> createFromRdfNodes(List<RDFNodeDto> nodes) throws CloneNotSupportedException {
        var biofidTaxon = new BiofidTaxon();
        for (var node : nodes) {
            if (node.getPredicate().getValue().endsWith("class")) biofidTaxon.setClazz(node.getObject().getValue());
            if (node.getPredicate().getValue().endsWith("family")) biofidTaxon.setFamily(node.getObject().getValue());
            if (node.getPredicate().getValue().endsWith("genus")) biofidTaxon.setGenus(node.getObject().getValue());
            if (node.getPredicate().getValue().endsWith("kingdom")) biofidTaxon.setKingdom(node.getObject().getValue());
            if (node.getPredicate().getValue().endsWith("order")) biofidTaxon.setOrder(node.getObject().getValue());
            if (node.getPredicate().getValue().endsWith("phylum")) biofidTaxon.setPhylum(node.getObject().getValue());
            if (node.getPredicate().getValue().endsWith("scientificName"))
                biofidTaxon.setScientificName(node.getObject().getValue());
            if (node.getPredicate().getValue().endsWith("cleanedScientificName"))
                biofidTaxon.setCleanedScientificName(node.getObject().getValue());
            if (node.getPredicate().getValue().endsWith("taxonRank")){
                var rank = ExceptionUtils.tryCatchLog(()-> TaxonRank.valueOf(node.getObject().getValue().toUpperCase()),(ex) -> {});
                if(rank != null) biofidTaxon.setTaxonRank(rank);
            }
            if (node.getPredicate().getValue().endsWith("scientificNameAuthorship"))
                biofidTaxon.setAuthor(node.getObject().getValue());
        }

        // We handle vernacular names as such, that we create a biofidTaxon object for each vernacular name.
        biofidTaxon.setPrimaryName(biofidTaxon.getCleanedScientificName());
        var biofidTaxons = new ArrayList<BiofidTaxon>();
        biofidTaxons.add(biofidTaxon);

        for (var node : nodes) {
            if (node.getPredicate().getValue().endsWith("vernacularName")) {
                BiofidTaxon duplicate = biofidTaxon.clone();
                duplicate.setVernacular(true);
                duplicate.setVernacularName(node.getObject().getValue());
                duplicate.setPrimaryName(node.getObject().getValue());
                biofidTaxons.add(duplicate);
            }
        }

        return biofidTaxons;
    }
}
