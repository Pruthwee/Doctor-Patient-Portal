package com.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		String host = System.getenv("ELASTICACHE_ENDPOINT");
		int port = 6379;
		String portStr = System.getenv("ELASTICACHE_PORT");
		if (portStr != null) {
			port = Integer.parseInt(portStr);
		}
		if (host == null) {
			host = "localhost";
		}
		return new LettuceConnectionFactory(host, port);
	}
}
