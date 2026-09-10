package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;

/**
 * Handles doctor password change requests.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that flash messages set via
 * {@code setAttribute} are propagated to the distributed Redis store instead
 * of being held in the local JVM heap. This allows any horizontally-scaled
 * instance to serve the subsequent redirect and read the flash message,
 * enabling stateless horizontal scaling without sticky sessions.
 *
 * <p>No changes to the session API calls are required – the Spring Session
 * {@code DelegatingFilterProxy} intercepts {@code req.getSession()} and
 * returns a Redis-backed {@link HttpSession} implementation automatically.
 */
@WebServlet("/doctorChangePassword")
public class DoctorChangePassword extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int doctorId = Integer.parseInt(req.getParameter("doctorId"));
		String newPassword = req.getParameter("newPassword");
		String oldPassword = req.getParameter("oldPassword");

		DoctorDAO doctorDAO = new DoctorDAO(DBConnection.getConn());

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session (springSessionRepositoryFilter in web.xml +
		// RedisSessionConfig). Flash messages stored here are written to the
		// distributed Redis store, enabling stateless horizontal scaling.
		HttpSession session = req.getSession();

		if (doctorDAO.checkOldPassword(doctorId, oldPassword)) {

			if (doctorDAO.changePassword(doctorId, newPassword)) {

				session.setAttribute("successMsg", "Password change successfully.");
				resp.sendRedirect("doctor/edit_profile.jsp");

			} else {

				session.setAttribute("errorMsg", "Something went wrong on server!");
				resp.sendRedirect("doctor/edit_profile.jsp");

			}

		} else {
			session.setAttribute("errorMsg", "Old Password not match");
			resp.sendRedirect("doctor/edit_profile.jsp");

		}
	}

}
