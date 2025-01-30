package org.texttechnologylab.models.imp;

import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="importlog")
public class ImportLog extends ModelBase {
    private String sender;
    private String message;
    private Long created;
    private LogStatus status;
    private String file;
    @Column(name = "uceimport_id")
    private Long uceImportId;
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

    public Long getUceImportId() {
        return uceImportId;
    }

    public void setUceImportId(Long uceImportId) {
        this.uceImportId = uceImportId;
    }
}
