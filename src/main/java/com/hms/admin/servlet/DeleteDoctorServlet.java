package com.hms.admin.servlet;

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
 * Handles deletion of a doctor record by the administrator.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that flash messages set via
 * {@code setAttribute} are propagated to the distributed Redis store instead
 * of being held in the local JVM heap. This allows any horizontally-scaled
 * instance to serve the subsequent redirect and read the flash message.
 */
@WebServlet("/deleteDoctor")
public class DeleteDoctorServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// Get id (which is coming as string value) and convert into int
		int id = Integer.parseInt(req.getParameter("id"));

		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session (springSessionRepositoryFilter in web.xml +
		// RedisSessionConfig). Flash messages stored here are written to the
		// distributed Redis store, enabling stateless horizontal scaling.
		HttpSession session = req.getSession();

		boolean f = docDAO.deleteDoctorById(id);

		if (f == true) {
			session.setAttribute("successMsg", "Doctor Deleted Successfully.");
			resp.sendRedirect("admin/view_doctor.jsp");
		} else {
			session.setAttribute("errorMsg", "Something went wrong on server!");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
	}
}
