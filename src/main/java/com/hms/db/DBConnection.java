package com.hms.db;

import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import java.time.Duration;
import com.google.gson.Gson;
import java.util.Map;

public class DBConnection {

	private static HikariDataSource dataSource;
	
	static {
		try {
			// Retrieve credentials from AWS Secrets Manager
			String secretName = System.getenv("DB_SECRET_NAME");
			String url = System.getenv("DB_URL");
			
					.overrideConfiguration(clientConfig)
					.build();

			GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
					.secretId(secretName)
					.build();
				
			GetSecretValueResponse valueResponse = client.getSecretValue(valueRequest);
			String secret = valueResponse.secretString();
			
			Gson gson = new Gson();
			Map<String, String> secretsMap = gson.fromJson(secret, Map.class);
			String username = secretsMap.get("username");
			String password = secretsMap.get("password");
			
			HikariConfig config = new HikariConfig();
			if (url == null) {
				url = "jdbc:mysql://localhost:3306/hospital";
			}
			config.setJdbcUrl(url);
			config.setUsername(username);
			config.setPassword(password);
			config.setDriverClassName("com.mysql.cj.jdbc.Driver");
			
			// Cloud-native optimizations for RDS Proxy / Connection Pooling
			config.setMaximumPoolSize(10);
			config.setMinimumIdle(2);
			config.setIdleTimeout(30000);
			config.setConnectionTimeout(30000);
			config.setLeakDetectionThreshold(2000);
			
			dataSource = new HikariDataSource(config);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Connection getConn() {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
}
