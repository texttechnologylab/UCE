package org.texttechnologylab.models.search;

public abstract class CacheItem {

    private boolean cleanupNextCycle;

    public boolean isCleanupNextCycle() {
        return cleanupNextCycle;
    }

    public void setCleanupNextCycle(boolean cleanupNextCycle) {
        this.cleanupNextCycle = cleanupNextCycle;
    }

    public abstract void dispose() throws Exception;
}
