package com.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Stores the HttpSession in Redis (Amazon ElastiCache) instead of the JVM heap,
 * so sessions survive pod restarts and are shared by all replicas.
 * The servlet code keeps using HttpSession: Spring Session wraps the request.
 *
 * Environment variables: REDIS_HOST, REDIS_PORT (default 6379),
 * REDIS_PASSWORD (optional, from a Secret), REDIS_SSL (true/false, default false).
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {

	@Bean
	public LettuceConnectionFactory connectionFactory() {
		String host = getEnv("REDIS_HOST", "localhost");
		int port = Integer.parseInt(getEnv("REDIS_PORT", "6379"));
		String password = getEnv("REDIS_PASSWORD", "");
		boolean ssl = Boolean.parseBoolean(getEnv("REDIS_SSL", "false"));

		RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(host, port);
		if (!password.isEmpty()) {
			server.setPassword(password);
		}

		LettuceClientConfiguration.LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder();
		if (ssl) {
			client.useSsl();
		}
		return new LettuceConnectionFactory(server, client.build());
	}

	private static String getEnv(String name, String defaultValue) {
		String value = System.getenv(name);
		return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
	}
}
