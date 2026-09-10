package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Handles user logout requests.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that session operations performed
 * here (removing {@code "userObj"} and setting {@code "successMsg"}) are
 * applied to the distributed Redis store instead of only the local JVM heap.
 * This ensures that session invalidation is propagated across all
 * horizontally-scaled instances without sticky sessions or server affinity.
 *
 * <p>No changes to the session API calls are required – the Spring Session
 * {@code DelegatingFilterProxy} intercepts {@code req.getSession()} and
 * returns a Redis-backed {@link HttpSession} implementation automatically.
 *
 * <h3>Required environment variables (resolved by {@code RedisSessionConfig})</h3>
 * <ul>
 *   <li>{@code REDIS_HOST} – ElastiCache primary endpoint hostname</li>
 *   <li>{@code REDIS_PORT} – ElastiCache port (default: 6379)</li>
 *   <li>{@code REDIS_PASSWORD} – ElastiCache AUTH token (optional)</li>
 * </ul>
 */
@WebServlet("/userLogout")
public class UserLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session (springSessionRepositoryFilter in web.xml +
		// RedisSessionConfig). Removing and setting attributes here operates
		// on the distributed Redis store, ensuring session state is consistent
		// across all stateless application instances.
		HttpSession session = req.getSession();
		session.removeAttribute("userObj");
		session.setAttribute("successMsg", "User Logout Successfully.");
		resp.sendRedirect("user_login.jsp");

	}

}
