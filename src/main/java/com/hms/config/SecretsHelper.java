package com.hms.config;

import java.time.Duration;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

/**
 * Reads secrets from AWS Secrets Manager. The client is built once, lazily, on the first
 * read (never at class load), so the application still starts when AWS cannot be reached.
 * Region and credentials come from the AWS SDK default provider chain (AWS_REGION, the
 * instance/task role, ...).
 */
public final class SecretsHelper {

	// all timeouts in milliseconds
	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int SOCKET_TIMEOUT_MS = 30000;
	private static final int API_CALL_TIMEOUT_MS = 60000;
	private static final int API_CALL_ATTEMPT_TIMEOUT_MS = 20000;

	private static SecretsManagerClient client;

	private SecretsHelper() {
	}

	private static synchronized SecretsManagerClient client() {
		if (client == null) {
			ApacheHttpClient.Builder httpClient = ApacheHttpClient.builder()
					.connectionTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
					.socketTimeout(Duration.ofMillis(SOCKET_TIMEOUT_MS));
			ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
					.apiCallTimeout(Duration.ofMillis(API_CALL_TIMEOUT_MS))
					.apiCallAttemptTimeout(Duration.ofMillis(API_CALL_ATTEMPT_TIMEOUT_MS)).build();
			client = SecretsManagerClient.builder().httpClientBuilder(httpClient)
					.overrideConfiguration(overrideConfig).build();
		}
		return client;
	}

	/**
	 * @param secretName name or ARN of the secret
	 * @return the secret's string value
	 * @throws software.amazon.awssdk.core.exception.SdkException when the secret cannot be read
	 */
	public static String getSecretString(String secretName) {
		return client().getSecretValue(GetSecretValueRequest.builder().secretId(secretName).build()).secretString();
	}

	public static synchronized void close() {
		if (client != null) {
			client.close();
			client = null;
		}
	}
}
