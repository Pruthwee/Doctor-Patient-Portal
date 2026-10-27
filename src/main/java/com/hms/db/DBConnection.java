package com.hms.db;

import java.sql.Connection;
import java.sql.SQLException;

import com.hms.config.SecretsHelper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;

public class DBConnection {

	// database location: DB_HOST, DB_PORT, DB_NAME, DB_USER (defaults are the old local values)
	private static final String DEFAULT_DB_HOST = "localhost";
	private static final String DEFAULT_DB_PORT = "3306";
	private static final String DEFAULT_DB_NAME = "hospital";
	private static final String DEFAULT_DB_USER = "root";

	// timeouts, all in milliseconds
	private static final int JDBC_CONNECT_TIMEOUT_MS = 5000;
	private static final int JDBC_SOCKET_TIMEOUT_MS = 30000;
	private static final long POOL_CONNECTION_TIMEOUT_MS = 5000;
	private static final long POOL_VALIDATION_TIMEOUT_MS = 3000;
	private static final long POOL_IDLE_TIMEOUT_MS = 600000;
	private static final long POOL_MAX_LIFETIME_MS = 1800000;
	private static final long POOL_LEAK_DETECTION_MS = 60000;
	private static final int POOL_MAX_SIZE = 20;
	private static final int POOL_MIN_IDLE = 5;

	// the pool is built lazily, on first use
	private static HikariDataSource dataSource;

	// one pooled connection per request thread; given back by ConnectionReleaseFilter at the end of the request
	private static final ThreadLocal<Connection> REQUEST_CONNECTION = new ThreadLocal<Connection>();

	public static Connection getConn() {

		try {

			Connection conn = REQUEST_CONNECTION.get();
			if (conn != null && !conn.isClosed()) {
				return conn;
			}

			conn = getDataSource().getConnection();
			REQUEST_CONNECTION.set(conn);
			return conn;

		} catch (SdkException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}

		return null;
	}

	/** Gives the connection of the current thread back to the pool. */
	public static void releaseConn() {
		Connection conn = REQUEST_CONNECTION.get();
		REQUEST_CONNECTION.remove();
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static synchronized void shutdown() {
		if (dataSource != null) {
			dataSource.close();
			dataSource = null;
		}
	}

	private static synchronized HikariDataSource getDataSource() throws SQLException {
		if (dataSource == null) {

			String secretName = System.getenv("DB_SECRET_NAME");
			if (secretName == null || secretName.trim().isEmpty()) {
				throw new SQLException("DB_SECRET_NAME is not set");
			}

			// the secret is a JSON document with "username" and "password"
			JsonNode secret = JsonNodeParser.create().parse(SecretsHelper.getSecretString(secretName));
			String user = env("DB_USER", DEFAULT_DB_USER);
			if (secret.field("username").isPresent()) {
				user = secret.field("username").get().asString();
			}
			String password = secret.field("password").get().asString();

			HikariConfig config = new HikariConfig();
			config.setJdbcUrl("jdbc:mysql://" + env("DB_HOST", DEFAULT_DB_HOST) + ":" + env("DB_PORT", DEFAULT_DB_PORT)
					+ "/" + env("DB_NAME", DEFAULT_DB_NAME));
			config.setUsername(user);
			config.setPassword(password);
			config.setDriverClassName("com.mysql.cj.jdbc.Driver");

			// the application starts without a database; the first query fails where the old connection failed
			config.setInitializationFailTimeout(-1);
			config.setMaximumPoolSize(POOL_MAX_SIZE);
			config.setMinimumIdle(POOL_MIN_IDLE);
			config.setConnectionTimeout(POOL_CONNECTION_TIMEOUT_MS);
			config.setValidationTimeout(POOL_VALIDATION_TIMEOUT_MS);
			config.setIdleTimeout(POOL_IDLE_TIMEOUT_MS);
			config.setMaxLifetime(POOL_MAX_LIFETIME_MS);
			config.setLeakDetectionThreshold(POOL_LEAK_DETECTION_MS);

			config.addDataSourceProperty("connectTimeout", String.valueOf(JDBC_CONNECT_TIMEOUT_MS));
			config.addDataSourceProperty("socketTimeout", String.valueOf(JDBC_SOCKET_TIMEOUT_MS));
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

			dataSource = new HikariDataSource(config);
		}
		return dataSource;
	}

	private static String env(String name, String defaultValue) {
		String value = System.getenv(name);
		return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
	}
}
