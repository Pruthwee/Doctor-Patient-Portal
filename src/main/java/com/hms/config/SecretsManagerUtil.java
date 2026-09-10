package com.hms.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for retrieving secrets from AWS Secrets Manager.
 * Replaces hardcoded credentials and API keys with centrally managed,
 * automatically rotatable secrets.
 *
 * <p>Secrets are stored in AWS Secrets Manager under a configurable secret name
 * (default: {@code hms/app/credentials}). The secret value is expected to be a
 * JSON object with the following keys:
 * <ul>
 *   <li>{@code admin_email}    – administrator login e-mail address</li>
 *   <li>{@code admin_password} – administrator login password</li>
 * </ul>
 *
 * <p>The AWS region is read from the environment variable {@code AWS_REGION}
 * (default: {@code us-east-1}).
 *
 * <p>Explicit connection and API-call timeouts are configured on the underlying
 * Apache HTTP client (cr-java-0097) to prevent indefinite hangs when the
 * Secrets Manager endpoint is unreachable or slow:
 * <ul>
 *   <li>{@code AWS_SM_CONNECT_TIMEOUT_MS}  – TCP connect timeout (default: 5000 ms)</li>
 *   <li>{@code AWS_SM_SOCKET_TIMEOUT_MS}   – socket read timeout (default: 10000 ms)</li>
 *   <li>{@code AWS_SM_API_CALL_TIMEOUT_MS} – total API call timeout (default: 15000 ms)</li>
 * </ul>
 */
public class SecretsManagerUtil {

    private static final Logger LOGGER = Logger.getLogger(SecretsManagerUtil.class.getName());

    // -----------------------------------------------------------------------
    // Environment-variable keys – AWS configuration
    // -----------------------------------------------------------------------
    /** Environment variable that overrides the default AWS region. */
    private static final String ENV_AWS_REGION  = "AWS_REGION";

    /** Environment variable that overrides the default secret name. */
    private static final String ENV_SECRET_NAME = "HMS_SECRET_NAME";

    // -----------------------------------------------------------------------
    // Environment-variable keys – timeout configuration (cr-java-0097)
    // -----------------------------------------------------------------------
    /** TCP connect timeout for the Secrets Manager HTTP client (milliseconds). */
    private static final String ENV_SM_CONNECT_TIMEOUT_MS  = "AWS_SM_CONNECT_TIMEOUT_MS";
    /** Socket read timeout for the Secrets Manager HTTP client (milliseconds). */
    private static final String ENV_SM_SOCKET_TIMEOUT_MS   = "AWS_SM_SOCKET_TIMEOUT_MS";
    /** Total API call timeout for a single Secrets Manager request (milliseconds). */
    private static final String ENV_SM_API_CALL_TIMEOUT_MS = "AWS_SM_API_CALL_TIMEOUT_MS";

    // -----------------------------------------------------------------------
    // Defaults
    // -----------------------------------------------------------------------
    /** Default secret name used when {@code HMS_SECRET_NAME} is not set. */
    private static final String DEFAULT_SECRET_NAME = "hms/app/credentials";

    /** Default AWS region used when {@code AWS_REGION} is not set. */
    private static final String DEFAULT_REGION = "us-east-1";

    /** Default TCP connect timeout: 5 seconds. */
    private static final long DEFAULT_SM_CONNECT_TIMEOUT_MS  = 5_000L;
    /** Default socket read timeout: 10 seconds. */
    private static final long DEFAULT_SM_SOCKET_TIMEOUT_MS   = 10_000L;
    /** Default total API call timeout: 15 seconds. */
    private static final long DEFAULT_SM_API_CALL_TIMEOUT_MS = 15_000L;

    // JSON field names stored inside the Secrets Manager secret value
    private static final String FIELD_ADMIN_EMAIL    = "admin_email";
    private static final String FIELD_ADMIN_PASSWORD = "admin_password";

    private SecretsManagerUtil() {
        // utility class – no instantiation
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns the administrator e-mail address stored in AWS Secrets Manager.
     *
     * @return admin e-mail, or {@code null} if the secret cannot be retrieved
     */
    public static String getAdminEmail() {
        return getSecretField(FIELD_ADMIN_EMAIL);
    }

    /**
     * Returns the administrator password stored in AWS Secrets Manager.
     *
     * @return admin password, or {@code null} if the secret cannot be retrieved
     */
    public static String getAdminPassword() {
        return getSecretField(FIELD_ADMIN_PASSWORD);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Retrieves the full secret JSON string from AWS Secrets Manager and
     * extracts the value for the requested {@code fieldName}.
     */
    private static String getSecretField(String fieldName) {
        try {
            String secretJson = getSecretValue();
            if (secretJson == null || secretJson.isEmpty()) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(secretJson);
            JsonNode node = root.get(fieldName);
            return (node != null && !node.isNull()) ? node.asText() : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse secret field '" + fieldName + "' from AWS Secrets Manager", e);
            return null;
        }
    }

    /**
     * Calls AWS Secrets Manager and returns the raw secret string.
     *
     * <p>The underlying Apache HTTP client is configured with explicit
     * connection and socket timeouts (cr-java-0097) so that the call cannot
     * hang indefinitely if the Secrets Manager endpoint is unreachable.
     * An overall API-call timeout is also applied via the SDK's
     * {@code overrideConfiguration} to bound the total request duration.
     */
    private static String getSecretValue() {
        String secretName = resolveSecretName();
        Region region     = resolveRegion();

        // cr-java-0097: resolve timeout values from environment variables
        long connectTimeoutMs  = envLong(ENV_SM_CONNECT_TIMEOUT_MS,  DEFAULT_SM_CONNECT_TIMEOUT_MS);
        long socketTimeoutMs   = envLong(ENV_SM_SOCKET_TIMEOUT_MS,   DEFAULT_SM_SOCKET_TIMEOUT_MS);
        long apiCallTimeoutMs  = envLong(ENV_SM_API_CALL_TIMEOUT_MS, DEFAULT_SM_API_CALL_TIMEOUT_MS);

        // cr-java-0097: configure Apache HTTP client with explicit connection
        // and socket timeouts to prevent indefinite hangs in cloud environments.
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder()
                .connectionTimeout(Duration.ofMillis(connectTimeoutMs))  // TCP connect timeout
                .socketTimeout(Duration.ofMillis(socketTimeoutMs));       // socket read/write timeout

        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .httpClientBuilder(httpClientBuilder)
                .build()) {

            // cr-java-0097: apply a total API-call timeout so the entire
            // request (including retries) is bounded in duration.
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofMillis(apiCallTimeoutMs)))
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            return response.secretString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Unable to retrieve secret '" + secretName + "' from AWS Secrets Manager in region " + region, e);
            return null;
        }
    }

    private static String resolveSecretName() {
        String name = System.getenv(ENV_SECRET_NAME);
        return (name != null && !name.isEmpty()) ? name : DEFAULT_SECRET_NAME;
    }

    private static Region resolveRegion() {
        String regionStr = System.getenv(ENV_AWS_REGION);
        if (regionStr != null && !regionStr.isEmpty()) {
            try {
                return Region.of(regionStr);
            } catch (Exception e) {
                LOGGER.warning("Invalid AWS_REGION value '" + regionStr + "', falling back to " + DEFAULT_REGION);
            }
        }
        return Region.of(DEFAULT_REGION);
    }

    // -----------------------------------------------------------------------
    // Environment-variable helpers
    // -----------------------------------------------------------------------

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
