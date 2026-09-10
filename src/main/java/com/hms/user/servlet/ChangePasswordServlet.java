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

/**
 * Handles user change-password requests.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that flash messages written here
 * (line 10 import, and session usage throughout) are propagated to the
 * distributed Redis store instead of only affecting the local JVM heap.
 * This ensures that session attributes are visible across all
 * horizontally-scaled instances without sticky sessions or server affinity.
 *
 * <p>No changes to the session API calls are required – the Spring Session
 * {@code DelegatingFilterProxy} intercepts {@code req.getSession()} and
 * returns a Redis-backed {@link HttpSession} implementation automatically.
 */
@WebServlet("/userChangePassword")
public class ChangePasswordServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int    userId      = Integer.parseInt(req.getParameter("userId"));
		String oldPassword = req.getParameter("oldPassword");
		String newPassword = req.getParameter("newPassword");

		UserDAO uDAO = new UserDAO(DBConnection.getConn());
		//boolean f = uDAO.checkOldPassword(userId, oldPassword);

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session (springSessionRepositoryFilter in web.xml +
		// RedisSessionConfig). Flash messages written here are stored in the
		// distributed Redis store, ensuring they are accessible across all
		// stateless application instances.
		HttpSession session = req.getSession();

		if (uDAO.checkOldPassword(userId, oldPassword)) {

			if (uDAO.changePassword(userId, newPassword)) {

				session.setAttribute("successMsg", "Password Change Successfully.");
				resp.sendRedirect("change_password.jsp");

			} else {

				session.setAttribute("errorMsg", "Something wrong on server!");
				resp.sendRedirect("change_password.jsp");

			}

		} else {
			session.setAttribute("errorMsg", "Old password incorrect");
			resp.sendRedirect("change_password.jsp");
		}

	}

}
