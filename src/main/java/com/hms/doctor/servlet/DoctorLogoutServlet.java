package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Handles doctor logout requests.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that session invalidation and
 * attribute removal performed here are propagated to the distributed Redis
 * store instead of only affecting the local JVM heap.
 * This ensures that logout is effective across all horizontally-scaled
 * instances without sticky sessions or server affinity.
 *
 * <p>No changes to the session API calls are required – the Spring Session
 * {@code DelegatingFilterProxy} intercepts {@code req.getSession()} and
 * returns a Redis-backed {@link HttpSession} implementation automatically.
 */
@WebServlet("/doctorLogout")
public class DoctorLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session (springSessionRepositoryFilter in web.xml +
		// RedisSessionConfig). Removing attributes and setting flash messages
		// here are written to the distributed Redis store, ensuring logout is
		// effective across all stateless application instances.
		HttpSession session = req.getSession();
		session.removeAttribute("doctorObj");
		session.setAttribute("successMsg", "Doctor Logout Successfully.");
		resp.sendRedirect("doctor_login.jsp");
	}

}
