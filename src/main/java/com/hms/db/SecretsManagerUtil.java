package com.hms.db;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.logging.Logger;

/**
 * Utility class for retrieving secrets from AWS Secrets Manager.
 * This replaces hardcoded credentials in source code with cloud-native
 * secret management, enabling centralized secret management, automatic
 * rotation, and audit logging.
 */
public class SecretsManagerUtil {

	private static final Logger logger = Logger.getLogger(SecretsManagerUtil.class.getName());

	private static AWSSecretsManager secretsManager;

	static {
		try {
			// Initialize the AWS Secrets Manager client using the default credential provider chain.
			// This supports environment variables, system properties, and IAM roles for EC2/ECS tasks.
			// Explicit connection and socket timeouts are configured to prevent indefinite hangs
			// in cloud environments with variable network latency.
			secretsManager = AWSSecretsManagerClientBuilder.standard()
					.withConnectionTimeout(30000)
					.withSocketTimeout(60000)
					.build();
		} catch (Exception e) {
			logger.severe("Failed to initialize AWS Secrets Manager client: " + e.getMessage());
		}
	}

	/**
	 * Retrieves a secret value from AWS Secrets Manager by secret name.
	 *
	 * @param secretName The name or ARN of the secret in AWS Secrets Manager
	 * @return The secret string value
	 */
	public static String getSecretString(String secretName) {
		if (secretsManager == null) {
			throw new IllegalStateException("AWS Secrets Manager client is not initialized");
		}

		GetSecretValueRequest request = new GetSecretValueRequest()
				.withSecretId(secretName);

		try {
			GetSecretValueResult result = secretsManager.getSecretValue(request);
			return result.getSecretString();
		} catch (AWSSecretsManagerException e) {
			logger.severe("Failed to retrieve secret '" + secretName + "': " + e.getMessage());
			throw e;
		}
	}

	/**
	 * Retrieves a specific field from a JSON-formatted secret stored in AWS Secrets Manager.
	 *
	 * @param secretName The name or ARN of the secret in AWS Secrets Manager
	 * @param field      The JSON field name to extract from the secret
	 * @return The value of the specified field
	 */
	public static String getSecretField(String secretName, String field) {
		String secretString = getSecretString(secretName);

		JsonParser parser = new JsonParser();
		JsonObject jsonObject = parser.parse(secretString).getAsJsonObject();

		return jsonObject.get(field).getAsString();
	}

	/**
	 * Retrieves the database secret from AWS Secrets Manager.
	 * The secret is expected to be a JSON object with fields:
	 * - username: database username
	 * - password: database password
	 * - engine: database engine
	 * - host: database host
	 * - port: database port
	 * - dbname: database name
	 *
	 * @return A DatabaseSecret object containing the database connection details
	 */
	public static DatabaseSecret getDatabaseSecret() {
		String secretName = System.getenv("DB_SECRET_NAME");
		if (secretName == null || secretName.isEmpty()) {
			secretName = "hospital/db/credentials";
		}

		String secretString = getSecretString(secretName);

		JsonParser parser = new JsonParser();
		JsonObject jsonObject = parser.parse(secretString).getAsJsonObject();

		DatabaseSecret dbSecret = new DatabaseSecret();
		dbSecret.setUsername(jsonObject.get("username").getAsString());
		dbSecret.setPassword(jsonObject.get("password").getAsString());
		dbSecret.setHost(jsonObject.get("host").getAsString());
		dbSecret.setPort(jsonObject.get("port").getAsInt());
		dbSecret.setDbname(jsonObject.get("dbname").getAsString());

		return dbSecret;
	}

	/**
	 * Simple container class for database connection secrets retrieved from AWS Secrets Manager.
	 */
	public static class DatabaseSecret {
		private String username;
		private String password;
		private String host;
		private int port;
		private String dbname;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getDbname() {
			return dbname;
		}

		public void setDbname(String dbname) {
			this.dbname = dbname;
		}
	}
}
