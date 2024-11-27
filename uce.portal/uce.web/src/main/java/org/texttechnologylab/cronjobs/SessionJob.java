package org.texttechnologylab.cronjobs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.texttechnologylab.SessionManager;

public class SessionJob implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    private long interval;

    public SessionJob(long interval){
        this.interval = interval;
    }

    public void run(){
        logger.info("Session CronJob has started.");
        while(true){
            try {
                logger.info("Session CronJob is still running and starting another cycle: " +
                        "Active Search States: " + SessionManager.ActiveSearches.size() + " | " +
                        "Cached Wiki Pages: " + SessionManager.CachedWikiPages.size());

                // Iterate and remove search states that are marked in the cycle
                var iterator = SessionManager.ActiveSearches.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    if (entry.getValue().isCleanupNextCycle()) {
                        iterator.remove();
                    } else{
                        entry.getValue().setCleanupNextCycle(true); // Else, mark it for the next cycle.
                    }
                }

                // Iterate and remove cached wiki page that are marked in the cycle
                var iterator2 = SessionManager.CachedWikiPages.entrySet().iterator();
                while (iterator2.hasNext()) {
                    var entry = iterator2.next();
                    if (entry.getValue().isCleanupNextCycle()) {
                        iterator2.remove(); // Remove the cached wiki page
                    } else{
                        entry.getValue().setCleanupNextCycle(true); // Else, mark it for the next cycle.
                    }
                }

                logger.info("Session CronJob is done with this cycle. " +
                        "Active Search States: " + SessionManager.ActiveSearches.size() + " | " +
                        "Cached Wiki Pages: " + SessionManager.CachedWikiPages.size());
                Thread.sleep(this.interval * 1000);
            } catch (Exception ex){
                logger.error("Session CronJob ran into an error. Continuing within the next cycle.", ex);
            }
        }
    }
}
