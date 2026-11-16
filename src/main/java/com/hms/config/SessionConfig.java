package com.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {

	@Bean
	public LettuceConnectionFactory connectionFactory() {
		String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
		String redisPortStr = System.getenv().getOrDefault("REDIS_PORT", "6379");
		int redisPort = Integer.parseInt(redisPortStr);
		String redisPassword = System.getenv("REDIS_PASSWORD");

		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
		if (redisPassword != null && !redisPassword.isEmpty()) {
			config.setPassword(RedisPassword.of(redisPassword));
		}
		return new LettuceConnectionFactory(config);
	}
}
