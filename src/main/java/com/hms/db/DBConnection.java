package com.hms.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

public class DBConnection {

	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int SOCKET_TIMEOUT_MS = 30000;

	private static DataSource dataSource;

	private DBConnection() {
	}

	public static Connection getConn() {
		try {
			if (dataSource == null) {
				dataSource = createDataSource();
			}
			return dataSource.getConnection();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static DataSource createDataSource() {
		try {
			HikariConfig config = new HikariConfig();
			config.setInitializationFailTimeout(-1);

			// Read credentials from AWS Secrets Manager (cr-java-0113: externalized secrets)
			String dbHost = System.getenv("DB_HOST");
			if (dbHost == null || dbHost.isEmpty()) {
				dbHost = "localhost";
			}
			String dbPort = System.getenv("DB_PORT");
			if (dbPort == null || dbPort.isEmpty()) {
				dbPort = "3306";
			}
			String dbName = System.getenv("DB_NAME");
			if (dbName == null || dbName.isEmpty()) {
				dbName = "hospital";
			}

			String jdbcUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;

			String secretName = System.getenv("DB_SECRET_NAME");
			String username = "";
			String password = "";

			if (secretName != null && !secretName.isEmpty()) {
				try {
					SecretsManagerClient secretsClient = SecretsManagerClient.create();
					GetSecretValueRequest request = GetSecretValueRequest.builder()
							.secretId(secretName)
							.build();
					GetSecretValueResponse response = secretsClient.getSecretValue(request);
					String secret = response.secretString();
					// Parse simple JSON for username and password
					username = parseJsonField(secret, "username");
					password = parseJsonField(secret, "password");
				} catch (SecretsManagerException e) {
					e.printStackTrace();
				}
			}

			config.setJdbcUrl(jdbcUrl);
			config.setUsername(username);
			config.setPassword(password);
			config.setDriverClassName("com.mysql.cj.jdbc.Driver");

			// Connection pool optimization (cr-java-0073: HikariCP connection pool)
			config.setMaximumPoolSize(20);
			config.setMinimumIdle(5);
			config.setConnectionTimeout(CONNECT_TIMEOUT_MS);
			config.setIdleTimeout(600000);
			config.setMaxLifetime(1800000);
			config.setLeakDetectionThreshold(60000);

			// Connection timeouts (cr-java-0097: Missing Connection Timeouts)
			config.addDataSourceProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_MS));
			config.addDataSourceProperty("socketTimeout", String.valueOf(SOCKET_TIMEOUT_MS));
			config.addDataSourceProperty("loginTimeout", "10000");
			config.addDataSourceProperty("tcpKeepAlive", "true");

			// Health check query
			config.setConnectionTestQuery("SELECT 1");

			return new HikariDataSource(config);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String parseJsonField(String json, String field) {
		String search = "\"" + field + "\":\"";
		int start = json.indexOf(search);
		if (start >= 0) {
			start += search.length();
			int end = json.indexOf("\"", start);
			if (end > start) {
				return json.substring(start, end);
			}
		}
		return "";
	}

	public static void shutdown() {
		if (dataSource instanceof HikariDataSource) {
			((HikariDataSource) dataSource).close();
		}
	}
}
