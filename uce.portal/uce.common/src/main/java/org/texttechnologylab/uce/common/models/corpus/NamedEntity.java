package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;

import javax.persistence.*;

@Entity
@Table(name = "namedEntity")
@Typesystem(types = {NamedEntity.class})
public class NamedEntity extends UIMAAnnotation implements WikiModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @OneToOne
    @JoinColumn(name = "geoname_id")
    private GeoName geoName;

    @Column(name = "geoname_id", insertable = false, updatable = false)
    private Long geoNameId;

    @Column(name = "\"typee\"")
    private String type;

    public NamedEntity() {
        super(-1, -1);
    }

    public NamedEntity(int begin, int end) {
        super(begin, end);
    }

    public GeoName getGeoName() {
        return geoName;
    }

    public void setGeoName(GeoName geoName) {
        this.geoName = geoName;
    }

    public Long getGeoNameId() {
        return geoNameId;
    }

    public void setGeoNameId(Long geoNameId) {
        this.geoNameId = geoNameId;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getWikiId() {
        return "NE" + "-" + this.getId();
    }
}
