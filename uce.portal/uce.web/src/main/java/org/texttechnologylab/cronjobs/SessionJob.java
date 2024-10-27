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
                logger.info("Session CronJob still running. Cleaning the search states now. Active Search States: " + SessionManager.ActiveSearches.size());

                // Iterate and remove search states that are marked in the cycle
                var iterator = SessionManager.ActiveSearches.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    if (entry.getValue().isCleanupNextCycle()) {
                        iterator.remove(); // Remove the search state
                    } else{
                        entry.getValue().setCleanupNextCycle(true); // Else, mark it for the next cycle.
                    }
                }

                logger.info("Session CronJob is done with this cycle. Active Search States: " + SessionManager.ActiveSearches.size());
                Thread.sleep(this.interval * 1000);
            } catch (Exception ex){
                logger.error("Session CronJob ran into an error. Continuing within the next cycle.", ex);
            }
        }
    }
}
