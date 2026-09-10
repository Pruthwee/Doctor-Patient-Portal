package com.hms.user.servlet;

import java.io.IOException;
import java.io.PrintWriter;

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
 * Handles new user registration requests.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that all session attribute
 * operations performed here ({@code "successMsg"} and {@code "errorMsg"})
 * are applied to the distributed Redis store instead of only the local JVM
 * heap.  This ensures session data is consistent across all
 * horizontally-scaled, stateless application instances without sticky
 * sessions or server affinity.
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
@WebServlet("/user_register")
public class UserRegisterServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// PrintWriter out = resp.getWriter();

			// get all data/value which is coming from signup.jsp page for new User
			// registration
			String fullName = req.getParameter("fullName");
			String email = req.getParameter("email");
			String password = req.getParameter("password");

			// Set all data to User Entity
			User user = new User(fullName, email, password);

			// Create Connection with DB
			UserDAO userDAO = new UserDAO(DBConnection.getConn());

			// HttpSession is transparently backed by Amazon ElastiCache for Redis
			// via Spring Session (springSessionRepositoryFilter in web.xml +
			// RedisSessionConfig). Setting attributes here operates on the
			// distributed Redis store, ensuring session state is consistent
			// across all stateless application instances.
			HttpSession session = req.getSession();

			// call userRegister() and pass user object to insert or save user into DB.
			boolean f = userDAO.userRegister(user); // userRegister() method return boolean type value

			if (f == true) {

				// successMsg stored in Redis-backed distributed session (cr-java-0065)
				session.setAttribute("successMsg", "Register Successfully");
				resp.sendRedirect("signup.jsp");//which page you want to show this msg
				//System.out.println("register successfull");
				// out.println("success");

			} else {

				// errorMsg stored in Redis-backed distributed session (cr-java-0065)
				session.setAttribute("errorMsg", "Something went wrong!");
				resp.sendRedirect("signup.jsp");//which page you want to show this msg

				//System.out.println("Error! Something went wrong");
				// out.println("error");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
