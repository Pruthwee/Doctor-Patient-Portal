package com.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession
public class SessionConfig {

	@Bean
	public LettuceConnectionFactory connectionFactory() {
		String host = System.getenv("REDIS_HOST");
		if (host == null || host.isEmpty()) {
			host = "localhost";
		}
		String portStr = System.getenv("REDIS_PORT");
		int port = 6379;
		if (portStr != null && !portStr.isEmpty()) {
			port = Integer.parseInt(portStr);
		}
		String password = System.getenv("REDIS_PASSWORD");

		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(host);
		config.setPort(port);
		if (password != null && !password.isEmpty()) {
			config.setPassword(RedisPassword.of(password));
		}

		return new LettuceConnectionFactory(config);
	}
}
