package org.texttechnologylab.uce.common.models.imp;

import org.texttechnologylab.uce.common.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="importlog")
public class ImportLog extends ModelBase {
    private String sender;
    @Column(columnDefinition = "TEXT")
    private String message;
    private Long created;
    private LogStatus status;
    private long duration;
    @Column(columnDefinition = "TEXT")
    private String file;
    private String importId;
    public ImportLog(String sender, String message, LogStatus status, String file, String importId, long duration){
        this.created = System.currentTimeMillis();
        this.sender = sender;
        this.message = message;
        this.status = status;
        this.file = file;
        this.importId = importId;
        this.duration = duration;
    }
    public ImportLog(){
        this.created = System.currentTimeMillis();
    }
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getCreated() {
        return created;
    }

    public LogStatus getStatus() {
        return status;
    }

    public void setStatus(LogStatus status) {
        this.status = status;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getImportId() {
        return importId;
    }

    public void setImportId(String importId) {
        this.importId = importId;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
