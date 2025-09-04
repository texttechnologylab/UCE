package org.texttechnologylab.uce.common.models.gbif;

import org.joda.time.DateTime;
import org.texttechnologylab.uce.common.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="gbifOccurrence")
/**
 * A gbif occurrence is a linkage of a taxon to a dataset. These are scraped using their API, see here:
 * https://api.gbif.org/v1/occurrence/search?limit=5&media_type=stillImage&taxon_key=6093134
 */
public class GbifOccurrence extends ModelBase {

    @Column(name = "gbiftaxonid")
    private long gbifTaxonId;
    private long occurrenceId;
    private DateTime importedDate;
    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    private DateTime dateIdentified;
    private double latitude;
    private double longitude;

    @Column(columnDefinition = "TEXT")
    private String country;

    @Column(columnDefinition = "TEXT")
    private String region;

    public GbifOccurrence(){

    }

    public long getGbifTaxonId() {
        return gbifTaxonId;
    }

    public void setGbifTaxonId(long gbifTaxonId) {
        this.gbifTaxonId = gbifTaxonId;
    }

    public long getOccurrenceId() {
        return occurrenceId;
    }

    public void setOccurrenceId(long occurrenceId) {
        this.occurrenceId = occurrenceId;
    }

    public DateTime getImportedDate() {
        return importedDate;
    }

    public void setImportedDate(DateTime importedDate) {
        this.importedDate = importedDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public DateTime getDateIdentified() {
        return dateIdentified;
    }

    public void setDateIdentified(DateTime dateIdentified) {
        this.dateIdentified = dateIdentified;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
