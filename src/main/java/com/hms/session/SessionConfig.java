package com.hms.session;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		String redisHost = System.getenv("ELASTICACHE_ENDPOINT");
		if (redisHost == null || redisHost.isEmpty()) {
			redisHost = "localhost";
		}
		return new LettuceConnectionFactory(redisHost, 6379);
	}
}
