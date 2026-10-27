package com.hms.config;

import java.time.Duration;

import javax.servlet.Filter;

import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * Stores the HTTP sessions in Amazon ElastiCache for Redis (Spring Session), so that no
 * instance holds session state. The servlets keep using HttpSession unchanged.
 *
 * Settings: ELASTICACHE_ENDPOINT (host of the Redis endpoint) and REDIS_SECRET_NAME (name of the
 * Secrets Manager secret that holds the Redis AUTH token). Connections are opened on first use.
 */
public final class RedisSessionConfig {

	private static final int REDIS_PORT = 6379;
	private static final int SESSION_TIMEOUT_SECONDS = 1800;
	private static final String SESSION_NAMESPACE = "spring:session";

	// timeouts, all in milliseconds
	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int COMMAND_TIMEOUT_MS = 5000;
	private static final int SHUTDOWN_TIMEOUT_MS = 2000;

	private static LettuceConnectionFactory connectionFactory;

	private RedisSessionConfig() {
	}

	/**
	 * @return the servlet filter that backs HttpSession with Redis, or null when no Redis
	 *         endpoint is configured or it cannot be set up (the container's own sessions are used then)
	 */
	public static Filter createSessionFilter() {
		String endpoint = System.getenv("ELASTICACHE_ENDPOINT");
		if (endpoint == null || endpoint.trim().isEmpty()) {
			System.err.println("ELASTICACHE_ENDPOINT is not set: HTTP sessions stay in the server's memory");
			return null;
		}

		try {
			RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(endpoint.trim(), REDIS_PORT);
			String secretName = System.getenv("REDIS_SECRET_NAME");
			if (secretName != null && !secretName.trim().isEmpty()) {
				redisConfig.setPassword(RedisPassword.of(SecretsHelper.getSecretString(secretName.trim())));
			}

			LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().useSsl().and()
					.commandTimeout(Duration.ofMillis(COMMAND_TIMEOUT_MS))
					.shutdownTimeout(Duration.ofMillis(SHUTDOWN_TIMEOUT_MS))
					.clientOptions(ClientOptions.builder().socketOptions(
							SocketOptions.builder().connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS)).build())
							.build())
					.build();

			connectionFactory = new LettuceConnectionFactory(redisConfig, clientConfig);
			connectionFactory.afterPropertiesSet();

			JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer(
					RedisSessionConfig.class.getClassLoader());
			RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
			template.setConnectionFactory(connectionFactory);
			template.setKeySerializer(new StringRedisSerializer());
			template.setHashKeySerializer(new StringRedisSerializer());
			template.setValueSerializer(valueSerializer);
			template.setHashValueSerializer(valueSerializer);
			template.afterPropertiesSet();

			RedisSessionRepository repository = new RedisSessionRepository(template);
			repository.setRedisKeyNamespace(SESSION_NAMESPACE);
			repository.setDefaultMaxInactiveInterval(Duration.ofSeconds(SESSION_TIMEOUT_SECONDS));

			return newFilter(repository);
		} catch (SdkException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.err.println("Redis sessions could not be set up: HTTP sessions stay in the server's memory");
		return null;
	}

	private static <S extends Session> Filter newFilter(SessionRepository<S> repository) {
		return new SessionRepositoryFilter<S>(repository);
	}

	public static void shutdown() {
		if (connectionFactory != null) {
			connectionFactory.destroy();
			connectionFactory = null;
		}
	}
}
