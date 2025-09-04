package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;

import javax.persistence.*;

@Entity
@Table(name = "geoname")
@Typesystem(types = {org.texttechnologylab.annotation.GeoNamesEntity.class})
public class GeoName extends UIMAAnnotation implements WikiModel {

    @Column(columnDefinition = "TEXT")
    private String name;

    private GeoNameFeatureClass featureClass;
    private String featureCode;
    private String countryCode;
    private String adm1;
    private String adm2;
    private String adm3;
    private String adm4;
    private double latitude;
    private double longitude;
    private double elevation;

    @OneToOne(mappedBy = "geoName", cascade = CascadeType.ALL, orphanRemoval = true)
    private NamedEntity refNamedEntity;

    public GeoName(){}
    public GeoName(int begin, int end){
        super(begin, end);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GeoNameFeatureClass getFeatureClass() {
        return featureClass;
    }

    public void setFeatureClass(GeoNameFeatureClass featureClass) {
        this.featureClass = featureClass;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getAdm1() {
        return adm1;
    }

    public void setAdm1(String adm1) {
        this.adm1 = adm1;
    }

    public String getAdm2() {
        return adm2;
    }

    public void setAdm2(String adm2) {
        this.adm2 = adm2;
    }

    public String getAdm3() {
        return adm3;
    }

    public void setAdm3(String adm3) {
        this.adm3 = adm3;
    }

    public String getAdm4() {
        return adm4;
    }

    public void setAdm4(String adm4) {
        this.adm4 = adm4;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public NamedEntity getRefNamedEntity() {
        return refNamedEntity;
    }

    public void setRefNamedEntity(NamedEntity refNamedEntity) {
        this.refNamedEntity = refNamedEntity;
    }

    @Override
    public String getWikiId() { return "LOC-" + this.getId();}
}
