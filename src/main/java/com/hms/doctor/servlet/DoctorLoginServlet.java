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
import com.hms.entity.Doctor;


/**
 * Servlet to handle doctor login.
 *
 * Session management is backed by Amazon ElastiCache for Redis via
 * Spring Session, enabling stateless application instances with
 * centralized, distributed session storage.
 */
@WebServlet("/doctorLogin")
public class DoctorLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// Get email and password coming from doctor_login.jsp page
		String email = req.getParameter("email");
		String password = req.getParameter("password");

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session for distributed, stateless session management.
		HttpSession session = req.getSession();

		// Create DB connection via HikariCP pool
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());
		
		// Call loginDoctor() method for doctor login
		Doctor doctor = docDAO.loginDoctor(email, password);

		if (doctor != null) {
			// Doctor is valid/exists – store the doctor object in the distributed
			// Redis-backed session so all application instances can access it.
			session.setAttribute("doctorObj", doctor);
			// Redirect to the doctor dashboard
			resp.sendRedirect("doctor/index.jsp");
		} else {
			session.setAttribute("errorMsg", "Invalid email or password");
			resp.sendRedirect("doctor_login.jsp");
		}

	}

}
