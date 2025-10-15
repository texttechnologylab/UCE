package org.texttechnologylab.uce.common.models.dto;

import java.util.List;

public class TopicModellingDto {
    private int status;
    private List<String> rakeKeywords;
    private List<String> yakeKeywords;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<String> getRakeKeywords() {
        return rakeKeywords;
    }

    public void setRakeKeywords(List<String> rakeKeywords) {
        this.rakeKeywords = rakeKeywords;
    }

    public List<String> getYakeKeywords() {
        return yakeKeywords;
    }

    public void setYakeKeywords(List<String> yakeKeywords) {
        this.yakeKeywords = yakeKeywords;
    }
}
