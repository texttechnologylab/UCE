package org.texttechnologylab.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.config.UceConfig;
import org.texttechnologylab.cronjobs.SystemJob;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.models.util.HealthStatus;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class SystemStatus {
    public static HealthStatus GbifServiceStatus = new HealthStatus();
    public static HealthStatus GoetheUniversityServiceStatus = new HealthStatus();
    public static HealthStatus JenaSparqlStatus = new HealthStatus();
    public static HealthStatus PostgresqlDbStatus = new HealthStatus();
    public static HealthStatus RagServiceStatus = new HealthStatus();
    public static HealthStatus UIMAService = new HealthStatus();
    public static HealthStatus S3StorageStatus = new HealthStatus();
    public static boolean LexiconIsCalculating = false;
    public static UceConfig UceConfig = null;
    private static final Logger logger = LogManager.getLogger(SystemStatus.class);

    public static void initSystemStatus(long cleanupInterval, ApplicationContext serviceContext) {
        Runnable runnable = new SystemJob(cleanupInterval, serviceContext);
        var sessionJob = new Thread(runnable);
        sessionJob.start();
    }

    /**
     * Executes the external database scripts for triggers, procedures and such.
     */
    public static void executeExternalDatabaseScripts(String path, PostgresqlDataInterface_Impl db) throws IOException {
        try (var fileStream = Files.list(Paths.get(path))) {
            fileStream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".sql"))
                    .sorted((f1, f2) -> {
                        try {
                            int n1 = Integer.parseInt(f1.getFileName().toString().split("_", 2)[0]);
                            int n2 = Integer.parseInt(f2.getFileName().toString().split("_", 2)[0]);
                            return Integer.compare(n1, n2);
                        } catch (Exception e) {
                            return 0; // fallback to equal if parsing fails
                        }
                    })
                    .forEach(file -> {
                        try {
                            String sqlContent = Files.readString(file);
                            db.executeSqlWithoutReturn(sqlContent);
                            logger.info("*--> Successfully executed: " + file.getFileName());
                        } catch (IOException | DatabaseOperationException ex) {
                            logger.error("Error trying to execute the database script " + file.getFileName(), ex);
                        }
                    });
        }

    }
}
