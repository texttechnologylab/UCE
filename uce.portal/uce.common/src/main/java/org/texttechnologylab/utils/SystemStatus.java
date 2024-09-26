package org.texttechnologylab.utils;

import org.texttechnologylab.models.util.HealthStatus;

public final class SystemStatus {
    public static HealthStatus GbifServiceStatus = new HealthStatus();
    public static HealthStatus GoetheUniversityServiceStatus = new HealthStatus();
    public static HealthStatus JenaSparsqlStatus = new HealthStatus();
    public static HealthStatus PostgresqlDbStatus = new HealthStatus();
    public static HealthStatus RagServiceStatus = new HealthStatus();
    public static HealthStatus UIMAService = new HealthStatus();

}
