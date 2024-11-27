package org.texttechnologylab.models.viewModels.wiki;

public class CachedWikiPage {

    private boolean cleanupNextCycle;
    private String renderedView;

    public CachedWikiPage(String renderedView){
        this.renderedView = renderedView;
    }

    public boolean isCleanupNextCycle() {
        return cleanupNextCycle;
    }

    public void setCleanupNextCycle(boolean cleanupNextCycle) {
        this.cleanupNextCycle = cleanupNextCycle;
    }

    public String getRenderedView() {
        return renderedView;
    }

    public void setRenderedView(String renderedView) {
        this.renderedView = renderedView;
    }
}
