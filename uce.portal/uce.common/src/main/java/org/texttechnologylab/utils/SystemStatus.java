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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class SystemStatus {
    public static HealthStatus GbifServiceStatus = new HealthStatus();
    public static HealthStatus GoetheUniversityServiceStatus = new HealthStatus();
    public static HealthStatus JenaSparqlStatus = new HealthStatus();
    public static HealthStatus PostgresqlDbStatus = new HealthStatus();
    public static HealthStatus RagServiceStatus = new HealthStatus();
    public static HealthStatus UIMAService = new HealthStatus();
    public static UceConfig UceConfig = null;
    private static final Logger logger = LogManager.getLogger(SystemStatus.class);

    public static void InitSystemStatus(long cleanupInterval, ApplicationContext serviceContext) {
        Runnable runnable = new SystemJob(cleanupInterval, serviceContext);
        var sessionJob = new Thread(runnable);
        sessionJob.start();
    }

    /**
     * Executes the external database scripts for triggers, procedures and such.
     */
    public static void ExecuteExternalDatabaseScripts(String path, PostgresqlDataInterface_Impl db) throws IOException {
        Files.list(Paths.get(path))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".sql"))
                .forEach(file -> {
                    try {
                        var sqlContent = Files.readString(file);
                        db.executeSqlWithoutReturn(sqlContent);
                        logger.info("*--> Successfully executed: " + file.getFileName());
                    } catch (IOException | DatabaseOperationException ex) {
                        logger.error("Error trying to execute the database script " + file.getFileName(), ex);
                    }
                });
    }
}
