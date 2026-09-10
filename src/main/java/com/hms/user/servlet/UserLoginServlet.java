package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.User;

/**
 * Handles user login requests.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that session attributes written
 * here (e.g. {@code "userObj"} and {@code "errorMsg"}) are propagated to the
 * distributed Redis store instead of only affecting the local JVM heap.
 * This ensures that session data is visible across all horizontally-scaled
 * instances without sticky sessions or server affinity.
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
@WebServlet("/userLogin")
public class UserLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String email    = req.getParameter("email");
		String password = req.getParameter("password");

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session (springSessionRepositoryFilter in web.xml +
		// RedisSessionConfig). Session attributes written here are stored in
		// the distributed Redis store, ensuring they are accessible across all
		// stateless application instances.
		HttpSession session = req.getSession();

		UserDAO userDAO = new UserDAO(DBConnection.getConn());
		User user = userDAO.loginUser(email, password);

		if (user != null) {
			session.setAttribute("userObj", user);
			resp.sendRedirect("index.jsp");
		} else {
			session.setAttribute("errorMsg", "Invalid email or password");
			resp.sendRedirect("user_login.jsp");
		}
	}

}
