package com.hms.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.time.Duration;

/**
 * Cloud-native database connection manager using HikariCP connection pool
 * with Amazon RDS Proxy support and AWS Secrets Manager for credential management.
 *
 * Replaces raw JDBC DriverManager connections with a properly configured
 * HikariCP pool that supports connection timeouts, health checks, and
 * cloud-optimized settings for Amazon RDS.
 */
public class DBConnection {

    private static volatile HikariDataSource dataSource;

    /**
     * Returns a connection from the HikariCP connection pool.
     * Credentials are retrieved from AWS Secrets Manager; connection
     * parameters fall back to environment variables when the secret
     * is unavailable (e.g., local development).
     */
    public static Connection getConn() {
        if (dataSource == null) {
            synchronized (DBConnection.class) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                }
            }
        }
        try {
            return dataSource.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to obtain database connection from pool", e);
        }
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();

        // Resolve DB credentials: prefer AWS Secrets Manager, fall back to env vars
        String dbUrl      = resolveDbUrl();
        String dbUsername = resolveDbUsername();
        String dbPassword = resolveDbPassword();

        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // ── Connection timeout settings (cloud-resilient) ──────────────────────
        // Maximum time to wait for a connection from the pool (30 s)
        config.setConnectionTimeout(30_000);
        // Maximum time a connection may sit idle in the pool (10 min)
        config.setIdleTimeout(600_000);
        // Maximum lifetime of a connection in the pool (30 min)
        config.setMaxLifetime(1_800_000);
        // Validation timeout when testing a connection (5 s)
        config.setValidationTimeout(5_000);
        // Keepalive interval to prevent cloud firewall from dropping idle connections (5 min)
        config.setKeepaliveTime(300_000);

        // ── Pool sizing ─────────────────────────────────────────────────────────
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        config.setPoolName("HmsHikariPool");

        // ── Connection health check ─────────────────────────────────────────────
        config.setConnectionTestQuery("SELECT 1");

        // ── Cloud-friendly JDBC properties ─────────────────────────────────────
        // Automatically reconnect on stale connections (important for RDS failover)
        config.addDataSourceProperty("autoReconnect", "true");
        config.addDataSourceProperty("useSSL", System.getenv().getOrDefault("DB_USE_SSL", "true"));
        config.addDataSourceProperty("requireSSL", System.getenv().getOrDefault("DB_REQUIRE_SSL", "false"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        // Socket-level read/write timeout (seconds) – prevents indefinite hangs
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("connectTimeout", "10");

        return new HikariDataSource(config);
    }

    // ── Credential resolution helpers ──────────────────────────────────────────

    private static String resolveDbUrl() {
        // 1. Try environment variable (12-factor / ECS task definition)
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.isEmpty()) {
            return envUrl;
        }
        // 2. Build from individual env vars (RDS Proxy endpoint pattern)
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "3306");
        String name = System.getenv().getOrDefault("DB_NAME", "hospital");
        return "jdbc:mysql://" + host + ":" + port + "/" + name;
    }

    private static String resolveDbUsername() {
        // 1. Try AWS Secrets Manager
        String secretUsername = getSecretValue("username");
        if (secretUsername != null) return secretUsername;
        // 2. Fall back to environment variable
        return System.getenv().getOrDefault("DB_USERNAME", "root");
    }

    private static String resolveDbPassword() {
        // 1. Try AWS Secrets Manager
        String secretPassword = getSecretValue("password");
        if (secretPassword != null) return secretPassword;
        // 2. Fall back to environment variable (never hardcode credentials)
        return System.getenv().getOrDefault("DB_PASSWORD", "");
    }

    /**
     * Retrieves a field from the AWS Secrets Manager secret whose name is
     * specified by the DB_SECRET_NAME environment variable.
     * Returns null if the secret name is not configured or retrieval fails.
     */
    private static String getSecretValue(String fieldName) {
        String secretName = System.getenv("DB_SECRET_NAME");
        if (secretName == null || secretName.isEmpty()) {
            return null;
        }
        try {
            String awsRegion = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(Region.of(awsRegion))
                    .overrideConfiguration(c -> c
                            .apiCallTimeout(Duration.ofSeconds(10))
                            .apiCallAttemptTimeout(Duration.ofSeconds(5)))
                    .build();

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(secretString);
            JsonNode fieldNode = node.get(fieldName);
            return (fieldNode != null) ? fieldNode.asText() : null;

        } catch (Exception e) {
            System.err.println("[DBConnection] Could not retrieve secret '" + secretName
                    + "' field '" + fieldName + "' from AWS Secrets Manager: " + e.getMessage());
            return null;
        }
    }
}
