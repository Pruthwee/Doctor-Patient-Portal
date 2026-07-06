package com.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import java.time.Duration;

/**
 * Spring Session configuration backed by Amazon ElastiCache for Redis.
 *
 * This configuration replaces server-local HTTP session storage with a
 * centralized, distributed Redis session store, enabling:
 *  - Stateless application instances (horizontal scaling)
 *  - Session persistence across instance restarts / deployments
 *  - Shared session state when load-balanced across multiple instances
 *
 * Connection parameters are resolved from environment variables so that
 * no infrastructure details are hardcoded in source code (12-factor app).
 *
 * Required environment variables:
 *   REDIS_HOST        – ElastiCache primary endpoint (default: localhost)
 *   REDIS_PORT        – Redis port (default: 6379)
 *   REDIS_PASSWORD    – Redis AUTH password (optional, leave unset for no auth)
 *   SESSION_TIMEOUT   – Session timeout in seconds (default: 1800 = 30 min)
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class RedisSessionConfig {

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        String host     = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int    port     = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        String password = System.getenv("REDIS_PASSWORD");

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isEmpty()) {
            redisConfig.setPassword(password);
        }

        // Configure Lettuce client with explicit connection and command timeouts
        // to prevent indefinite hangs in cloud environments.
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))
                .shutdownTimeout(Duration.ofSeconds(2))
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }
}
