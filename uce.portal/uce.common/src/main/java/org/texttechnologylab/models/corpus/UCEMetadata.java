package org.texttechnologylab.models.corpus;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.viewModels.JsonViewModel;
import org.texttechnologylab.utils.JsonBeautifier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ucemetadata")
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
        return beautifier.parseJsonToViewModel(jsonString);
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
