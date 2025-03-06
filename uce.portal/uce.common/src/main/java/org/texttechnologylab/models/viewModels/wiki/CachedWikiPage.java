package org.texttechnologylab.models.viewModels.wiki;


import org.texttechnologylab.models.search.CacheItem;

public class CachedWikiPage extends CacheItem {

    private String renderedView;

    public CachedWikiPage(String renderedView) {
        this.renderedView = renderedView;
    }

    public String getRenderedView() {
        return renderedView;
    }

    public void setRenderedView(String renderedView) {
        this.renderedView = renderedView;
    }

    public void dispose() { }
}
