package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * Through this class, we allow UCE to have metadata about the corpus, that is dynamic.
 * Since we don't know the data UCE imports, we annotate some metadata and import that. That again, can be used
 * as filtering or sorting options in the UI later.
 */
@Entity
@Table(name="ucemetadatafilter")
public class UCEMetadataFilter extends ModelBase {
    private long corpusId;
    private String key;
    private UCEMetadataValueType valueType;
    private Float min;
    private Float max;

    public Float getMax() {
        return max;
    }

    public void setMax(Float max) {
        this.max = max;
    }

    public Float getMin() {
        return min;
    }

    public void setMin(Float min) {
        this.min = min;
    }

    /**
     * This is only relevant if the UCEMetadataValueType is of Type Enum, since then we have set categories as a filter
     * (Similar to a dropdown)
     */
    @Column(columnDefinition = "TEXT")
    private String possibleCategories;

    public UCEMetadataFilter(long corpusId, String key, UCEMetadataValueType valueType){
        this.corpusId = corpusId;
        this.key = key;
        this.valueType = valueType;
        if(valueType == UCEMetadataValueType.ENUM) this.possibleCategories = "";
    }

    public UCEMetadataFilter() {

    }

    public void addPossibleCategory(String category){
        if(category.isBlank() || category.isEmpty() || this.valueType != UCEMetadataValueType.ENUM) return;
        this.possibleCategories += category + ";;;";
    }

    public long getCorpusId() {
        return corpusId;
    }

    public void setCorpusId(long corpusId) {
        this.corpusId = corpusId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public UCEMetadataValueType getValueType() {
        return valueType;
    }

    public void setValueType(UCEMetadataValueType valueType) {
        this.valueType = valueType;
    }

    public List<String> getPossibleCategories() {
        return Arrays.stream(possibleCategories.split(";;;")).toList();
    }

    public void setPossibleCategories(List<String> possibleCategories) {
        this.possibleCategories = String.join(";;;", possibleCategories);
    }
}
