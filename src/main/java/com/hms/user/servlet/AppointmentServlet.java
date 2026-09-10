package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Appointment;

/**
 * Handles patient appointment booking requests.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that flash messages written here
 * (lines 40, 44, 51) are propagated to the distributed Redis store instead of
 * only affecting the local JVM heap.
 * This ensures that session attributes are visible across all
 * horizontally-scaled instances without sticky sessions or server affinity.
 *
 * <p>No changes to the session API calls are required – the Spring Session
 * {@code DelegatingFilterProxy} intercepts {@code req.getSession()} and
 * returns a Redis-backed {@link HttpSession} implementation automatically.
 */
@WebServlet("/addAppointment")
public class AppointmentServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int    userId          = Integer.parseInt(req.getParameter("userId"));
		String fullName        = req.getParameter("fullName");
		String gender          = req.getParameter("gender");
		String age             = req.getParameter("age");
		String appointmentDate = req.getParameter("appointmentDate");
		String email           = req.getParameter("email");
		String phone           = req.getParameter("phone");
		String diseases        = req.getParameter("diseases");
		int    doctorId        = Integer.parseInt(req.getParameter("doctorNameSelect"));
		String address         = req.getParameter("address");

		Appointment appointment = new Appointment(userId, fullName, gender, age, appointmentDate, email, phone, diseases, doctorId, address, "Pending");

		AppointmentDAO appointmentDAO = new AppointmentDAO(DBConnection.getConn());
		boolean f = appointmentDAO.addAppointment(appointment);

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session (springSessionRepositoryFilter in web.xml +
		// RedisSessionConfig). Flash messages written here are stored in the
		// distributed Redis store, ensuring they are accessible across all
		// stateless application instances.
		HttpSession session = req.getSession();

		if (f == true) {

			session.setAttribute("successMsg", "Appointment is recorded Successfully.");
			resp.sendRedirect("user_appointment.jsp");

		} else {

			session.setAttribute("errorMsg", "Something went wrong on server!");
			resp.sendRedirect("user_appointment.jsp");

		}

	}

}
