package com.hms.db;

import java.sql.Connection;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DBConnection {

	private static final String DB_SECRET_NAME = System.getenv("DB_SECRET_NAME");
	private static final int CONNECTION_TIMEOUT_MS = 5000;
	private static final int SOCKET_TIMEOUT_MS = 5000;

	private static HikariDataSource dataSource;

	private static HikariDataSource getDataSource() {
		if (dataSource == null) {
			try {
				HikariConfig config = new HikariConfig();

				// Read database credentials from AWS Secrets Manager
				String dbHost = "localhost";
				String dbPort = "3306";
				String dbName = "hospital";
				String dbUser = "root";
				String dbPassword = "";

				if (DB_SECRET_NAME != null && !DB_SECRET_NAME.isEmpty()) {
					SecretsManagerClient secretsClient = SecretsManagerClient.create();
					GetSecretValueRequest request = GetSecretValueRequest.builder()
							.secretId(DB_SECRET_NAME)
							.build();
					GetSecretValueResponse response = secretsClient.getSecretValue(request);
					String secretString = response.secretString();

					ObjectMapper mapper = new ObjectMapper();
					JsonNode secretJson = mapper.readTree(secretString);
					dbHost = secretJson.get("host").asText();
					dbPort = String.valueOf(secretJson.get("port").asInt());
					dbName = secretJson.get("database").asText();
					dbUser = secretJson.get("username").asText();
					dbPassword = secretJson.get("password").asText();
				}

				String jdbcUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
				config.setJdbcUrl(jdbcUrl);
				config.setUsername(dbUser);
				config.setPassword(dbPassword);
				config.setDriverClassName("com.mysql.cj.jdbc.Driver");

				// Connection pool optimization for cloud environments
				config.setMaximumPoolSize(20);
				config.setMinimumIdle(5);
				config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
				config.setIdleTimeout(600000);
				config.setMaxLifetime(1800000);
				config.setLeakDetectionThreshold(60000);
				config.setInitializationFailTimeout(-1);

				// MySQL Connector/J connection properties for timeouts
				config.addDataSourceProperty("connectTimeout", String.valueOf(CONNECTION_TIMEOUT_MS));
				config.addDataSourceProperty("socketTimeout", String.valueOf(SOCKET_TIMEOUT_MS));
				config.setConnectionTestQuery("SELECT 1");

				dataSource = new HikariDataSource(config);
			} catch (SecretsManagerException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dataSource;
	}

	private static Connection conn;

	public static Connection getConn() {

		try {

			//step:1 for connection - load the driver class
			Class.forName("com.mysql.cj.jdbc.Driver");

			//step:2- create a connection via HikariCP pool
			DataSource ds = getDataSource();
			if (ds != null) {
				conn = ds.getConnection();
			}

		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}

		return conn;
	}
}
