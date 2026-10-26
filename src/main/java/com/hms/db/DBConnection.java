package com.hms.db;

import java.sql.Connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.core.exception.SdkException;

public class DBConnection {

	private static final long CONNECT_TIMEOUT_MS = 5000;
	private static final long SOCKET_TIMEOUT_MS = 30000;
	private static final long LOGIN_TIMEOUT_SEC = 10;

	private static HikariDataSource dataSource;

	private static synchronized HikariDataSource getDataSource() {
		if (dataSource == null) {
			try {
				// Read database credentials from AWS Secrets Manager (cr-java-0113)
				String dbHost = System.getenv("DB_HOST");
				String dbPort = System.getenv("DB_PORT");
				String dbName = System.getenv("DB_NAME");
				String dbUser = System.getenv("DB_USER");
				String secretName = System.getenv("DB_SECRET_NAME");

				if (dbHost == null || dbPort == null || dbName == null || dbUser == null || secretName == null) {
					throw new IllegalStateException("Database environment variables are not set");
				}

				String dbPassword = getSecretPassword(secretName);

				String jdbcUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
						+ "?connectTimeout=" + CONNECT_TIMEOUT_MS
						+ "&socketTimeout=" + SOCKET_TIMEOUT_MS
						+ "&loginTimeout=" + LOGIN_TIMEOUT_SEC;

				HikariConfig config = new HikariConfig();
				config.setJdbcUrl(jdbcUrl);
				config.setUsername(dbUser);
				config.setPassword(dbPassword);
				config.setDriverClassName("com.mysql.cj.jdbc.Driver");

				// Connection pool optimization for cloud environments (cr-java-0073)
				config.setMaximumPoolSize(20);
				config.setMinimumIdle(5);
				config.setConnectionTimeout(CONNECT_TIMEOUT_MS);
				config.setIdleTimeout(600000);
				config.setMaxLifetime(1800000);
				config.setLeakDetectionThreshold(60000);
				// Start without requiring a connection at startup (cr-java-0073)
				config.setInitializationFailTimeout(-1);

				// Query timeout (cr-java-0097)
				config.setConnectionTestQuery("SELECT 1");

				dataSource = new HikariDataSource(config);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dataSource;
	}

	private static String getSecretPassword(String secretName) {
		try {
			SecretsManagerClient secretsClient = SecretsManagerClient.builder().build();
			GetSecretValueRequest request = GetSecretValueRequest.builder()
					.secretId(secretName)
					.build();
			GetSecretValueResponse response = secretsClient.getSecretValue(request);
			secretsClient.close();
			return response.secretString();
		} catch (SdkException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static Connection getConn() {
		try {
			HikariDataSource ds = getDataSource();
			if (ds == null) {
				return null;
			}
			return ds.getConnection();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
