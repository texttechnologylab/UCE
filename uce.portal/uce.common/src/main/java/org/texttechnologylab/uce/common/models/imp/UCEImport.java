package org.texttechnologylab.uce.common.models.imp;

import org.texttechnologylab.uce.common.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;

@Entity
@Table(name = "uceimport")
public class UCEImport extends ModelBase {
    private String importId;
    private Integer totalDocuments;
    private long created;
    private String basePath;
    private ImportStatus status;
    private Long targetCorpusId;
    private String targetCorpusName;
    @Column(columnDefinition = "TEXT")
    private String comment;
    @Transient
    private List<ImportLog> logs;

    public UCEImport(String importId, String basePath, ImportStatus status){
        this.importId = importId;
        this.basePath = basePath;
        this.status = status;
        this.created = System.currentTimeMillis();
    }

    public UCEImport() {
        this.created = System.currentTimeMillis();
    }

    public String getTargetCorpusName() {
        return targetCorpusName;
    }

    public void setTargetCorpusName(String targetCorpusName) {
        this.targetCorpusName = targetCorpusName;
    }

    public String getImportId() {
        return importId;
    }

    public void setImportId(String importId) {
        this.importId = importId;
    }

    public Integer getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(Integer totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getCreated() {
        return created;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public void setStatus(ImportStatus status) {
        this.status = status;
    }

    public Long getTargetCorpusId() {
        return targetCorpusId;
    }

    public void setTargetCorpusId(Long targetCorpusId) {
        this.targetCorpusId = targetCorpusId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<ImportLog> getLogs() {
        return logs;
    }

    public void setLogs(List<ImportLog> logs) {
        this.logs = logs;
    }
}
