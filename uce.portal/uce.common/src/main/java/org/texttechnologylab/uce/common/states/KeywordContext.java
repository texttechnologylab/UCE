package org.texttechnologylab.uce.common.states;

import java.util.List;

public class KeywordContext {

    private String keyword;
    private List<String> before;
    private List<String> after;
    private long document_id;

    public String getAfterString(){
        return String.join(" ", after);
    }

    public String getBeforeString(){
        return String.join(" ",before);
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getBefore() {
        return before;
    }

    public void setBefore(List<String> before) {
        this.before = before;
    }

    public List<String> getAfter() {
        return after;
    }

    public void setAfter(List<String> after) {
        this.after = after;
    }

    public long getDocument_id() {
        return document_id;
    }

    public void setDocument_id(long document_id) {
        this.document_id = document_id;
    }
}
