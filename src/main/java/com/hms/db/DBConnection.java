package com.hms.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Cloud-ready database connection manager.
 *
 * <p>Replaces raw JDBC DriverManager connections with a HikariCP connection
 * pool. The pool is configured entirely through environment variables so that
 * no credentials or host information are hard-coded in source. When deployed
 * to AWS the JDBC URL can point to an Amazon RDS Proxy endpoint, which gives
 * the application efficient, auto-scaling database connections.</p>
 *
 * <p>Configuration environment variables (all optional with sensible defaults):</p>
 * <ul>
 *   <li>{@code DB_DRIVER_CLASS}   - JDBC driver class (default: com.mysql.cj.jdbc.Driver)</li>
 *   <li>{@code DB_JDBC_URL}       - JDBC connection URL (default: jdbc:mysql://localhost:3306/hospital)</li>
 *   <li>{@code DB_USERNAME}       - Database username (default: root)</li>
 *   <li>{@code DB_PASSWORD}       - Database password (default: empty)</li>
 *   <li>{@code DB_POOL_NAME}      - Hikari pool name (default: HikariCP)</li>
 *   <li>{@code DB_POOL_SIZE}      - Maximum pool size (default: 10)</li>
 *   <li>{@code DB_MIN_IDLE}       - Minimum idle connections (default: 2)</li>
 *   <li>{@code DB_CONNECTION_TIMEOUT_MS} - Connection timeout in ms (default: 30000)</li>
 *   <li>{@code DB_IDLE_TIMEOUT_MS}       - Idle timeout in ms (default: 600000)</li>
 *   <li>{@code DB_MAX_LIFETIME_MS}       - Max lifetime in ms (default: 1800000)</li>
 *   <li>{@code DB_SOCKET_TIMEOUT_MS}     - Socket read timeout in ms (default: 30000)</li>
 *   <li>{@code DB_LEAK_DETECTION_THRESHOLD_MS} - Leak detection threshold in ms (default: 60000)</li>
 * </ul>
 */
public class DBConnection {

	private static volatile DataSource dataSource;

	/**
	 * Lazily initializes and returns the singleton HikariCP DataSource.
	 *
	 * <p>The DataSource is created once and reused for the lifetime of the
	 * application, providing efficient connection pooling. In an AWS
	 * environment the {@code DB_JDBC_URL} should reference an RDS Proxy
	 * endpoint to leverage managed connection pooling.</p>
	 *
	 * @return a pooled {@link Connection} from the HikariCP DataSource
	 */
	public static Connection getConn() {
		try {
			return getDataSource().getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the singleton HikariCP DataSource, creating it on first use.
	 *
	 * @return the configured {@link DataSource}
	 */
	private static DataSource getDataSource() {
		if (dataSource == null) {
			synchronized (DBConnection.class) {
				if (dataSource == null) {
					dataSource = createDataSource();
				}
			}
		}
		return dataSource;
	}

	/**
	 * Builds a HikariCP DataSource from environment-variable configuration.
	 *
	 * @return a new {@link HikariDataSource}
	 */
	private static DataSource createDataSource() {
		HikariConfig config = new HikariConfig();

		// Database connection details - sourced from environment variables
		String driverClass = System.getenv().getOrDefault("DB_DRIVER_CLASS", "com.mysql.cj.jdbc.Driver");
		String jdbcUrl = System.getenv().getOrDefault("DB_JDBC_URL", "jdbc:mysql://localhost:3306/hospital");
		String username = System.getenv().getOrDefault("DB_USERNAME", "root");
		String password = System.getenv().getOrDefault("DB_PASSWORD", "");

		config.setDriverClassName(driverClass);
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);

		// Pool sizing and lifecycle - configurable via environment variables
		config.setPoolName(System.getenv().getOrDefault("DB_POOL_NAME", "HikariCP"));
		config.setMaximumPoolSize(parseIntEnv("DB_POOL_SIZE", 10));
		config.setMinimumIdle(parseIntEnv("DB_MIN_IDLE", 2));
		config.setConnectionTimeout(parseIntEnv("DB_CONNECTION_TIMEOUT_MS", 30000));
		config.setIdleTimeout(parseIntEnv("DB_IDLE_TIMEOUT_MS", 600000));
		config.setMaxLifetime(parseIntEnv("DB_MAX_LIFETIME_MS", 1800000));
		// Socket timeout - prevents indefinite hangs on network reads
		config.addDataSourceProperty("socketTimeout", parseIntEnv("DB_SOCKET_TIMEOUT_MS", 30000));
		config.addDataSourceProperty("connectTimeout", parseIntEnv("DB_CONNECTION_TIMEOUT_MS", 30000));
		config.setLeakDetectionThreshold(parseIntEnv("DB_LEAK_DETECTION_THRESHOLD_MS", 60000));

		// Recommended HikariCP settings for cloud environments
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		config.addDataSourceProperty("rewriteBatchedStatements", "true");

		return new HikariDataSource(config);
	}

	/**
	 * Parses an integer from an environment variable, falling back to a default.
	 *
	 * @param name         the environment variable name
	 * @param defaultValue the default value if unset or invalid
	 * @return the parsed integer value
	 */
	private static int parseIntEnv(String name, int defaultValue) {
		String value = System.getenv(name);
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Closes the HikariCP DataSource on application shutdown.
	 * This should be called from a shutdown hook or context listener.
	 */
	public static void close() {
		if (dataSource instanceof HikariDataSource) {
			((HikariDataSource) dataSource).close();
			dataSource = null;
		}
	}
}
