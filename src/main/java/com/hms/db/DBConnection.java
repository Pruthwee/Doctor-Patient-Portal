package com.hms.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cloud-ready database connection provider using HikariCP connection pooling
 * combined with Amazon RDS Proxy for optimised database connections in AWS.
 *
 * <p>All connection parameters are resolved from environment variables so that
 * no credentials or host names are hard-coded in the application binary:
 *
 * <ul>
 *   <li>{@code DB_HOST}     – RDS Proxy endpoint or RDS instance hostname
 *                             (default: {@code localhost})</li>
 *   <li>{@code DB_PORT}     – database port (default: {@code 3306})</li>
 *   <li>{@code DB_NAME}     – database / schema name (default: {@code hospital})</li>
 *   <li>{@code DB_USER}     – database username (default: {@code root})</li>
 *   <li>{@code DB_PASSWORD} – database password (default: empty string)</li>
 * </ul>
 *
 * <p>HikariCP pool settings can be tuned via the following optional environment
 * variables:
 * <ul>
 *   <li>{@code DB_POOL_MAX_SIZE}              – maximum pool size (default: 10)</li>
 *   <li>{@code DB_POOL_MIN_IDLE}              – minimum idle connections (default: 2)</li>
 *   <li>{@code DB_POOL_CONNECTION_TIMEOUT_MS} – connection-acquisition timeout in ms
 *                                              (default: 30000)</li>
 *   <li>{@code DB_POOL_IDLE_TIMEOUT_MS}       – idle connection timeout in ms
 *                                              (default: 600000)</li>
 *   <li>{@code DB_POOL_MAX_LIFETIME_MS}       – maximum connection lifetime in ms
 *                                              (default: 1800000)</li>
 *   <li>{@code DB_SOCKET_TIMEOUT_MS}          – JDBC socket read/write timeout in ms
 *                                              (default: 30000)</li>
 *   <li>{@code DB_CONNECT_TIMEOUT_MS}         – JDBC TCP connect timeout in ms
 *                                              (default: 10000)</li>
 * </ul>
 *
 * <p>When deployed behind <strong>Amazon RDS Proxy</strong> the proxy endpoint
 * should be supplied via {@code DB_HOST}. RDS Proxy handles connection
 * multiplexing at the infrastructure level; HikariCP still provides
 * application-level pooling and health-checking.
 */
public class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    // -----------------------------------------------------------------------
    // Environment-variable keys – connection coordinates
    // -----------------------------------------------------------------------
    private static final String ENV_DB_HOST     = "DB_HOST";
    private static final String ENV_DB_PORT     = "DB_PORT";
    private static final String ENV_DB_NAME     = "DB_NAME";
    private static final String ENV_DB_USER     = "DB_USER";
    private static final String ENV_DB_PASSWORD = "DB_PASSWORD";

    // -----------------------------------------------------------------------
    // Environment-variable keys – HikariCP pool settings
    // -----------------------------------------------------------------------
    private static final String ENV_POOL_MAX_SIZE        = "DB_POOL_MAX_SIZE";
    private static final String ENV_POOL_MIN_IDLE        = "DB_POOL_MIN_IDLE";
    private static final String ENV_POOL_CONN_TIMEOUT_MS = "DB_POOL_CONNECTION_TIMEOUT_MS";
    private static final String ENV_POOL_IDLE_TIMEOUT_MS = "DB_POOL_IDLE_TIMEOUT_MS";
    private static final String ENV_POOL_MAX_LIFETIME_MS = "DB_POOL_MAX_LIFETIME_MS";

    // -----------------------------------------------------------------------
    // Environment-variable keys – socket / network timeouts (cr-java-0097)
    // -----------------------------------------------------------------------
    /** JDBC socket read/write timeout (milliseconds). Prevents indefinite hangs on slow/failed DB. */
    private static final String ENV_SOCKET_TIMEOUT_MS  = "DB_SOCKET_TIMEOUT_MS";
    /** JDBC TCP connect timeout (milliseconds). Limits time spent establishing the TCP connection. */
    private static final String ENV_CONNECT_TIMEOUT_MS = "DB_CONNECT_TIMEOUT_MS";

    // -----------------------------------------------------------------------
    // Defaults – connection coordinates
    // -----------------------------------------------------------------------
    private static final String DEFAULT_HOST     = "localhost";
    private static final String DEFAULT_PORT     = "3306";
    private static final String DEFAULT_DB_NAME  = "hospital";
    private static final String DEFAULT_USER     = "root";
    private static final String DEFAULT_PASSWORD = "";

    // -----------------------------------------------------------------------
    // Defaults – HikariCP pool settings
    // -----------------------------------------------------------------------
    private static final int  DEFAULT_POOL_MAX_SIZE    = 10;
    private static final int  DEFAULT_POOL_MIN_IDLE    = 2;
    private static final long DEFAULT_CONN_TIMEOUT_MS  = 30_000L;
    private static final long DEFAULT_IDLE_TIMEOUT_MS  = 600_000L;
    private static final long DEFAULT_MAX_LIFETIME_MS  = 1_800_000L;

    // -----------------------------------------------------------------------
    // Defaults – socket / network timeouts (cr-java-0097)
    // -----------------------------------------------------------------------
    /** Default JDBC socket timeout: 30 seconds. Prevents connections hanging indefinitely. */
    private static final int DEFAULT_SOCKET_TIMEOUT_MS  = 30_000;
    /** Default JDBC connect timeout: 10 seconds. Limits TCP handshake wait time. */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;

    // -----------------------------------------------------------------------
    // Singleton HikariCP data source – initialised once on first use
    // -----------------------------------------------------------------------
    private static volatile HikariDataSource dataSource;

    private DBConnection() {
        // utility class – no instantiation
    }

    /**
     * Returns a {@link Connection} obtained from the HikariCP connection pool.
     *
     * <p>The pool is initialised lazily on the first call using a
     * double-checked locking pattern so that it is safe for concurrent
     * servlet requests.
     *
     * @return a live {@link Connection} from the pool, or {@code null} if the
     *         pool could not be initialised or a connection could not be
     *         acquired (callers should check for {@code null})
     */
    public static Connection getConn() {
        if (dataSource == null) {
            synchronized (DBConnection.class) {
                if (dataSource == null) {
                    dataSource = buildDataSource();
                }
            }
        }

        if (dataSource == null) {
            LOGGER.severe("HikariCP data source is not available – cannot provide a database connection.");
            return null;
        }

        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to acquire a connection from the HikariCP pool", e);
            return null;
        }
    }

    /**
     * Shuts down the HikariCP connection pool gracefully.
     * Should be called from a {@code ServletContextListener} on application
     * shutdown to release all pooled connections cleanly.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP connection pool shut down.");
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Builds and returns a configured {@link HikariDataSource}.
     *
     * <p>All parameters are read from environment variables so that the
     * application binary contains no hard-coded credentials or host names.
     *
     * <p>Explicit connection and socket timeouts are set at both the HikariCP
     * pool level and the JDBC URL level (cr-java-0097) to prevent connections
     * from hanging indefinitely in cloud environments with variable network
     * latency or transient service failures:
     * <ul>
     *   <li>HikariCP {@code connectionTimeout} – max wait to acquire a pooled
     *       connection from the pool queue.</li>
     *   <li>MySQL JDBC {@code connectTimeout}  – max time for the TCP handshake
     *       to the database host.</li>
     *   <li>MySQL JDBC {@code socketTimeout}   – max time waiting for a response
     *       on an established socket (read/write timeout).</li>
     * </ul>
     */
    private static HikariDataSource buildDataSource() {
        try {
            String host     = env(ENV_DB_HOST,     DEFAULT_HOST);
            String port     = env(ENV_DB_PORT,     DEFAULT_PORT);
            String dbName   = env(ENV_DB_NAME,     DEFAULT_DB_NAME);
            String user     = env(ENV_DB_USER,     DEFAULT_USER);
            String password = env(ENV_DB_PASSWORD, DEFAULT_PASSWORD);

            // cr-java-0097: resolve socket-level timeout values from env vars
            int socketTimeoutMs  = envInt(ENV_SOCKET_TIMEOUT_MS,  DEFAULT_SOCKET_TIMEOUT_MS);
            int connectTimeoutMs = envInt(ENV_CONNECT_TIMEOUT_MS, DEFAULT_CONNECT_TIMEOUT_MS);

            // JDBC URL – includes explicit connectTimeout and socketTimeout
            // parameters so that every physical connection has bounded network
            // wait times, preventing indefinite hangs in cloud environments.
            // When DB_HOST points to an RDS Proxy endpoint the proxy handles
            // IAM authentication and connection multiplexing.
            String jdbcUrl = String.format(
                    "jdbc:mysql://%s:%s/%s"
                    + "?useSSL=true&requireSSL=false"
                    + "&serverTimezone=UTC"
                    + "&allowPublicKeyRetrieval=true"
                    + "&connectTimeout=%d"    // TCP connect timeout (ms) – cr-java-0097
                    + "&socketTimeout=%d",    // socket read/write timeout (ms) – cr-java-0097
                    host, port, dbName,
                    connectTimeoutMs,
                    socketTimeoutMs);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Pool sizing
            config.setMaximumPoolSize(envInt(ENV_POOL_MAX_SIZE,  DEFAULT_POOL_MAX_SIZE));
            config.setMinimumIdle(    envInt(ENV_POOL_MIN_IDLE,  DEFAULT_POOL_MIN_IDLE));

            // HikariCP-level timeouts (cr-java-0097)
            // connectionTimeout: max ms to wait for a connection from the pool
            config.setConnectionTimeout(envLong(ENV_POOL_CONN_TIMEOUT_MS, DEFAULT_CONN_TIMEOUT_MS));
            // idleTimeout: how long a connection may sit idle before being retired
            config.setIdleTimeout(      envLong(ENV_POOL_IDLE_TIMEOUT_MS, DEFAULT_IDLE_TIMEOUT_MS));
            // maxLifetime: upper bound on a connection's total lifetime in the pool
            config.setMaxLifetime(      envLong(ENV_POOL_MAX_LIFETIME_MS, DEFAULT_MAX_LIFETIME_MS));

            // Health-check query
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("HmsHikariPool");

            LOGGER.info(String.format(
                    "Initialising HikariCP pool → %s:%s/%s  [connectTimeout=%dms, socketTimeout=%dms]",
                    host, port, dbName, connectTimeoutMs, socketTimeoutMs));

            return new HikariDataSource(config);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise HikariCP connection pool", e);
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Environment-variable helpers
    // -----------------------------------------------------------------------

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private static int envInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid integer value for env var '" + key + "': " + value
                        + " – using default " + defaultValue);
            }
        }
        return defaultValue;
    }

    private static long envLong(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid long value for env var '" + key + "': " + value
                        + " – using default " + defaultValue);
            }
        }
        return defaultValue;
    }
}
