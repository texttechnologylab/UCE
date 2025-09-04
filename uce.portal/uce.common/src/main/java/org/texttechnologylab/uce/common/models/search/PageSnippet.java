package org.texttechnologylab.uce.common.models.search;

import org.texttechnologylab.uce.common.models.corpus.Page;

public class PageSnippet {

    private int pageId;
    private transient Page page;
    private String snippet;

    public PageSnippet(){}

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
