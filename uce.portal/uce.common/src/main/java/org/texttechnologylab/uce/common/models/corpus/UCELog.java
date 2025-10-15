package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="ucelog")
public class UCELog extends ModelBase {
    private String ip;
    private String method;
    private String uri;
    @Column(columnDefinition = "TEXT")
    private String query;
    @Column(columnDefinition = "TEXT")
    private String body;
    private Long created;
    public UCELog(){}

    public UCELog(String ip, String method, String uri, String body, String query){
        this.ip = ip;
        this.method = method;
        this.uri = uri;
        this.body = body;
        this.query = query;
        this.created = System.currentTimeMillis();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getCreated() {
        return created;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
