package org.texttechnologylab.cronjobs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.services.JenaSparqlService;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;
import org.texttechnologylab.utils.SystemStatus;

public class SystemJob implements Runnable {

    private static final Logger logger = LogManager.getLogger(SystemJob.class);

    private final long interval;
    private final ApplicationContext serviceContext;

    public SystemJob(long interval, ApplicationContext serviceContext) {
        this.interval = interval;
        this.serviceContext = serviceContext;
    }

    public void run() {
        logger.info("System CronJob has started.");

        while (true) {
            try {
                // First, check all service connections - maybe a new one has connected
                // DB
                this.serviceContext.getBean(PostgresqlDataInterface_Impl.class).TestConnection();

                // Jena sparql
                this.serviceContext.getBean(JenaSparqlService.class).TestConnection();

                // RAG
                this.serviceContext.getBean(RAGService.class).TestConnection();

                logger.info("System CronJob is still running and has finished a cycle.");
                Thread.sleep(this.interval * 1000);
            } catch (Exception ex) {
                logger.error("System CronJob ran into an error. Continuing within the next cycle.", ex);
            }
        }
    }

}
