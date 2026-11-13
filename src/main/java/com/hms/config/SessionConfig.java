package com.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Spring Session configuration that externalizes HttpSession storage to Redis.
 * This replaces in-memory session storage (cz-java-0063, cz-java-0069) with
 * Redis-backed sessions via Spring Session, enabling horizontal scaling on EKS.
 *
 * Redis connection properties are read from environment variables:
 *   REDIS_HOST     - Redis host (default: localhost)
 *   REDIS_PORT     - Redis port (default: 6379)
 *   REDIS_PASSWORD - Redis password (optional)
 */
@Configuration
@EnableRedisHttpSession
public class SessionConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");

        int port = 6379;
        String portStr = System.getenv("REDIS_PORT");
        if (portStr != null && !portStr.isEmpty()) {
            port = Integer.parseInt(portStr);
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);

        String password = System.getenv("REDIS_PASSWORD");
        if (password != null && !password.isEmpty()) {
            config.setPassword(RedisPassword.of(password));
        }

        return new LettuceConnectionFactory(config);
    }
}
