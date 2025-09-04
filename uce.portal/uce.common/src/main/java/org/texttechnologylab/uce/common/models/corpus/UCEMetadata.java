package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.viewModels.JsonViewModel;
import org.texttechnologylab.uce.common.utils.JsonBeautifier;
import org.texttechnologylab.uce.common.utils.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "ucemetadata")
@Typesystem(types = {org.texttechnologylab.annotation.uce.Metadata.class})
public class UCEMetadata extends ModelBase {
    @Column(name = "document_id", insertable = false, updatable = false)
    private long documentId;
    private String key;
    @Column(columnDefinition = "TEXT")
    private String value;
    private UCEMetadataValueType valueType;
    @Column(columnDefinition = "TEXT")
    private String comment;

    public List<JsonViewModel> getJsonValueAsIterable() {
        if (this.valueType != UCEMetadataValueType.JSON) return null;
        var jsonString = this.value;
        if (jsonString == null || jsonString.isBlank()) return null;

        var beautifier = new JsonBeautifier();
        return beautifier
                .parseJsonToViewModel(jsonString)
                .stream()
                .sorted(Comparator
                        .comparing(JsonViewModel::getValueType)
                        .thenComparing(filter -> {
                            // Try to extract a number in the beginning of the key
                            String key = filter.getKey();

                            // TODO this is a special case for Coh-Metrix, should be generalized
                            // TODO duplicated in "Corpus getUceMetadataFilters"
                            if (key.contains(":")) {
                                String[] parts = key.split(":");
                                if (parts.length > 1) {
                                    try {
                                        int number = Integer.parseInt(parts[0].trim());
                                        return String.format("%05d", number);
                                    } catch (NumberFormatException e) {
                                        // return the original key on error
                                    }
                                }
                            }

                            return key;
                        })
                )
                .toList();
    }

    public long getDocumentId() {
        return this.documentId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets a potentially cleaner version of the value, e.g. decimal numbers are rounded and the likes.
     */
    public String getCleanValue(){
        if(getValue() == null) return "";
        if(valueType == UCEMetadataValueType.NUMBER){
            return StringUtils.tryRoundToTwoDecimals(getValue());
        }
        return getValue();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public UCEMetadataValueType getValueType() {
        return valueType;
    }

    public void setValueType(UCEMetadataValueType valueType) {
        this.valueType = valueType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
