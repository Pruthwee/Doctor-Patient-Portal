package com.hms.db;

import java.sql.Connection;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.hms.db.SecretsManagerUtil.DatabaseSecret;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DBConnection {

	private static final Logger logger = Logger.getLogger(DBConnection.class.getName());

	private static HikariDataSource dataSource;

	static {
		try {
			// Retrieve database credentials from AWS Secrets Manager instead of hardcoding them.
			// This enables centralized secret management, automatic rotation, and audit logging.
			DatabaseSecret dbSecret = SecretsManagerUtil.getDatabaseSecret();

			String host = dbSecret.getHost();
			int port = dbSecret.getPort();
			String dbname = dbSecret.getDbname();
			String username = dbSecret.getUsername();
			String password = dbSecret.getPassword();

			// Build the JDBC URL from the secret values.
			// If an RDS Proxy endpoint is provided via environment variable, use it
			// for optimized connection pooling and failover in AWS cloud environments.
			String rdsProxyEndpoint = System.getenv("RDS_PROXY_ENDPOINT");
			String jdbcUrl;
			if (rdsProxyEndpoint != null && !rdsProxyEndpoint.isEmpty()) {
				jdbcUrl = "jdbc:mysql://" + rdsProxyEndpoint + "/" + dbname + "?connectTimeout=30000&socketTimeout=60000&loginTimeout=30";
			} else {
				jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbname + "?connectTimeout=30000&socketTimeout=60000&loginTimeout=30";
			}

			// Configure HikariCP connection pool for efficient resource utilization,
			// proper connection lifecycle management, and integration with cloud
			// database connection pooling mechanisms (e.g., Amazon RDS Proxy).
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(jdbcUrl);
			config.setUsername(username);
			config.setPassword(password);
			config.setDriverClassName("com.mysql.cj.jdbc.Driver");

			// Pool sizing - optimized for cloud environments
			config.setMaximumPoolSize(20);
			config.setMinimumIdle(5);
			config.setConnectionTimeout(30000);
			config.setIdleTimeout(600000);
			config.setMaxLifetime(1800000);
			config.setLeakDetectionThreshold(60000);

			// Enable connection validation for cloud reliability
			config.setConnectionTestQuery("SELECT 1");

			dataSource = new HikariDataSource(config);

			logger.info("HikariCP connection pool initialized successfully.");

		} catch (Exception e) {
			logger.severe("Failed to initialize HikariCP connection pool: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Returns a connection from the HikariCP connection pool.
	 * The pool is initialized once at class load time and reused
	 * across all requests, providing efficient connection lifecycle
	 * management and integration with cloud database services.
	 *
	 * @return a pooled Connection from HikariCP
	 */
	public static Connection getConn() {
		try {
			if (dataSource == null) {
				throw new IllegalStateException("HikariCP DataSource has not been initialized");
			}
			return dataSource.getConnection();
		} catch (Exception e) {
			logger.severe("Failed to obtain database connection from pool: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the underlying DataSource for applications that need
	 * direct DataSource access (e.g., for transaction management).
	 *
	 * @return the HikariCP DataSource
	 */
	public static DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Closes the HikariCP connection pool. Should be called during
	 * application shutdown to release all pooled connections.
	 */
	public static void closePool() {
		if (dataSource != null) {
			dataSource.close();
			logger.info("HikariCP connection pool closed.");
		}
	}
}
