package org.texttechnologylab.uce.web.cronjobs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.search.CacheItem;
import org.texttechnologylab.uce.web.SessionManager;

import java.util.HashMap;

public class SessionJob implements Runnable {

    private static final Logger logger = LogManager.getLogger(SessionJob.class);

    private long interval;

    public SessionJob(long interval) {
        this.interval = interval;
    }

    public void run() {
        logger.info("Session CronJob has started.");
        while (true) {
            try {
                logger.info("Session CronJob is still running and starting another cycle: " +
                        "Active Search States: " + SessionManager.ActiveSearches.size() + " | " +
                        "Cached Wiki Pages: " + SessionManager.CachedWikiPages.size());

                // Iterate and remove search states that are marked in the cycle
                executeCleanupCycle(SessionManager.ActiveSearches);
                executeCleanupCycle(SessionManager.CachedWikiPages);
                executeCleanupCycle(SessionManager.ActiveLayeredSearches);

                logger.info("Session CronJob is done with this cycle. " +
                        "Active Search States: " + SessionManager.ActiveSearches.size() + " | " +
                        "Active Layered Searches: " + SessionManager.ActiveLayeredSearches.size() + " | " +
                        "Cached Wiki Pages: " + SessionManager.CachedWikiPages.size());
                Thread.sleep(this.interval * 1000);
            } catch (Exception ex) {
                logger.error("Session CronJob ran into an unknown error that shouldn't exist. Continuing within the next cycle.", ex);
            }
        }
    }

    private void executeCleanupCycle(HashMap<String, CacheItem> cachedItems) {
        var iterator = cachedItems.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isCleanupNextCycle()) {
                ExceptionUtils.tryCatchLog(
                        () -> {
                            entry.getValue().dispose();
                            iterator.remove();
                        },
                        (ex) -> logger.warn("Tried to cleanup a cached item, but got an error. Couldn't dispose it. Trying again in next cycle.", ex));
            } else {
                entry.getValue().setCleanupNextCycle(true);
            }
        }
    }
}
