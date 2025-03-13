package org.texttechnologylab.models.biofid;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.corpus.Page;
import org.texttechnologylab.models.dto.rdf.RDFNodeDto;

import javax.persistence.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "biofidtaxon")
public class BiofidTaxon extends UIMAAnnotation implements Cloneable {

    @Override
    public BiofidTaxon clone(){
        try{
            return (BiofidTaxon) super.clone();
        } catch (CloneNotSupportedException ex){
            throw new RuntimeException("Cloning of BiofidTaxon failed", ex);
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "document_id", insertable = false, updatable = false)
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @Column(name = "page_id", insertable = false, updatable = false)
    private Long pageId;

    private String biofidUrl;
    private String primaryName;
    private String vernacularName;
    private String scientificName;
    private String cleanedScientificName;
    private String kingdom;
    private String phylum;
    private String clazz;
    @Column(name = "orderr")
    private String order;
    private String family;
    private String genus;
    private TaxonRank taxonRank;
    private String author;
    private boolean isVernacular;

    public BiofidTaxon(int begin, int end) {
        super(begin, end);
    }

    public BiofidTaxon() {
    }

    public static List<BiofidTaxon> createFromRdfNodes(List<RDFNodeDto> nodes) throws CloneNotSupportedException {
        var biofidTaxon = new BiofidTaxon();
        for(var node:nodes){
            if(node.getPredicate().getValue().endsWith("class")) biofidTaxon.setClazz(node.getObject().getValue());
            if(node.getPredicate().getValue().endsWith("family")) biofidTaxon.setFamily(node.getObject().getValue());
            if(node.getPredicate().getValue().endsWith("genus")) biofidTaxon.setGenus(node.getObject().getValue());
            if(node.getPredicate().getValue().endsWith("kingdom")) biofidTaxon.setKingdom(node.getObject().getValue());
            if(node.getPredicate().getValue().endsWith("order")) biofidTaxon.setOrder(node.getObject().getValue());
            if(node.getPredicate().getValue().endsWith("phylum")) biofidTaxon.setPhylum(node.getObject().getValue());
            if(node.getPredicate().getValue().endsWith("scientificName")) biofidTaxon.setScientificName(node.getObject().getValue());
            if(node.getPredicate().getValue().endsWith("cleanedScientificName")) biofidTaxon.setCleanedScientificName(node.getObject().getValue());
            if(node.getPredicate().getValue().endsWith("taxonRank")) biofidTaxon.setTaxonRank(TaxonRank.valueOf(node.getObject().getValue().toUpperCase()));
            if(node.getPredicate().getValue().endsWith("scientificNameAuthorship")) biofidTaxon.setAuthor(node.getObject().getValue());
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

    public String getPrimaryName() {
        return primaryName;
    }

    public void setPrimaryName(String primaryName) {
        this.primaryName = primaryName;
    }

    public String getVernacularName() {
        return vernacularName;
    }

    public void setVernacularName(String vernacularName) {
        this.vernacularName = vernacularName;
    }

    public boolean isVernacular() {
        return isVernacular;
    }

    public void setVernacular(boolean vernacular) {
        isVernacular = vernacular;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public String getBiofidUrl() {
        return biofidUrl;
    }

    public void setBiofidUrl(String biofidUrl) {
        this.biofidUrl = biofidUrl;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getCleanedScientificName() {
        return cleanedScientificName;
    }

    public void setCleanedScientificName(String cleanedScientificName) {
        this.cleanedScientificName = cleanedScientificName;
    }

    public String getKingdom() {
        return kingdom;
    }

    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }

    public String getPhylum() {
        return phylum;
    }

    public void setPhylum(String phylum) {
        this.phylum = phylum;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public TaxonRank getTaxonRank() {
        return taxonRank;
    }

    public void setTaxonRank(TaxonRank taxonRank) {
        this.taxonRank = taxonRank;
    }
}
