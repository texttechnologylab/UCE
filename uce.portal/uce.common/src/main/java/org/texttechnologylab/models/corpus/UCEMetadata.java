package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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
