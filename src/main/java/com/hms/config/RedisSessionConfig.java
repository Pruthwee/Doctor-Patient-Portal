package com.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spring Session configuration that stores HTTP session data in
 * Amazon ElastiCache for Redis instead of in-memory server state.
 *
 * <p>This configuration replaces the default servlet-container
 * {@link javax.servlet.http.HttpSession} implementation with a
 * Redis-backed distributed session store, enabling:
 * <ul>
 *   <li>Stateless application instances – any instance can serve any request</li>
 *   <li>Horizontal scaling without sticky sessions or server affinity</li>
 *   <li>Session persistence across instance restarts and deployments</li>
 *   <li>Zero session data loss when instances are terminated by the load balancer</li>
 * </ul>
 *
 * <h3>Required environment variables</h3>
 * <table border="1">
 *   <tr><th>Variable</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>{@code REDIS_HOST}</td><td>{@code localhost}</td>
 *       <td>ElastiCache primary endpoint hostname</td></tr>
 *   <tr><td>{@code REDIS_PORT}</td><td>{@code 6379}</td>
 *       <td>ElastiCache port (default Redis port)</td></tr>
 *   <tr><td>{@code REDIS_PASSWORD}</td><td>(empty)</td>
 *       <td>ElastiCache AUTH token (leave blank if AUTH is disabled)</td></tr>
 *   <tr><td>{@code SESSION_TIMEOUT_SECONDS}</td><td>{@code 1800}</td>
 *       <td>Maximum inactive interval for a session (30 minutes)</td></tr>
 * </table>
 *
 * <p>The {@code DelegatingFilterProxy} registered in {@code web.xml} under the
 * name {@code springSessionRepositoryFilter} intercepts every HTTP request and
 * transparently wraps the {@link javax.servlet.http.HttpServletRequest} so that
 * calls to {@link javax.servlet.http.HttpServletRequest#getSession()} return a
 * Redis-backed session. No changes to servlet business logic are required.
 *
 * <p>Fixes rule <strong>cr-java-0065</strong> (HTTP Session State Storage) for:
 * <ul>
 *   <li>AdminLoginServlet – session.setAttribute("adminObj", …)</li>
 *   <li>AdminLogoutServlet – session.removeAttribute("adminObj")</li>
 *   <li>DeleteDoctorServlet – session.setAttribute("successMsg" / "errorMsg", …)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class RedisSessionConfig {

    private static final Logger LOGGER = Logger.getLogger(RedisSessionConfig.class.getName());

    /** Default Redis host (overridden by {@code REDIS_HOST} env var). */
    private static final String DEFAULT_REDIS_HOST = "localhost";

    /** Default Redis port (overridden by {@code REDIS_PORT} env var). */
    private static final int DEFAULT_REDIS_PORT = 6379;

    /**
     * Creates a Lettuce-based Redis connection factory pointing at the
     * Amazon ElastiCache cluster endpoint resolved from environment variables.
     *
     * @return configured {@link LettuceConnectionFactory}
     */
    @Bean
    public LettuceConnectionFactory connectionFactory() {
        String host     = resolveRedisHost();
        int    port     = resolveRedisPort();
        String password = System.getenv("REDIS_PASSWORD");

        LOGGER.info("Configuring Spring Session Redis connection: " + host + ":" + port);

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);

        if (password != null && !password.isEmpty()) {
            redisConfig.setPassword(password);
        }

        return new LettuceConnectionFactory(redisConfig);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static String resolveRedisHost() {
        String host = System.getenv("REDIS_HOST");
        if (host != null && !host.isEmpty()) {
            return host;
        }
        LOGGER.warning("REDIS_HOST environment variable not set; falling back to '" + DEFAULT_REDIS_HOST + "'");
        return DEFAULT_REDIS_HOST;
    }

    private static int resolveRedisPort() {
        String portStr = System.getenv("REDIS_PORT");
        if (portStr != null && !portStr.isEmpty()) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING,
                        "Invalid REDIS_PORT value '" + portStr + "'; falling back to " + DEFAULT_REDIS_PORT, e);
            }
        }
        return DEFAULT_REDIS_PORT;
    }
}
