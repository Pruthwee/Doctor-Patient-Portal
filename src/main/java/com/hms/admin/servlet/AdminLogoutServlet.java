package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Handles administrator logout.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that {@code removeAttribute} and
 * {@code setAttribute} operations are propagated to the distributed Redis
 * store. Invalidating or removing attributes here removes them from Redis,
 * ensuring clean logout across all horizontally-scaled instances.
 */
@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session (springSessionRepositoryFilter in web.xml +
		// RedisSessionConfig). Removing "adminObj" here removes it from the
		// distributed Redis store, completing logout across all instances.
		HttpSession session = req.getSession();
		session.removeAttribute("adminObj");

		// Show message after logout – stored in Redis-backed session.
		session.setAttribute("successMsg", "Admin Logout Successfully");
		resp.sendRedirect("admin_login.jsp");
	}
}
