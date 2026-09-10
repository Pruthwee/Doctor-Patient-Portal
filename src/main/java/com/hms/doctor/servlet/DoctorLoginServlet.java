package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.DoctorDAO;
import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;

/**
 * Handles doctor login requests.
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is
 * stored in <strong>Amazon ElastiCache for Redis</strong> via Spring Session.
 * The {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently wraps {@link HttpSession} so that the authenticated
 * {@code doctorObj} set via {@code setAttribute} is propagated to the
 * distributed Redis store instead of being held in the local JVM heap.
 * This allows any horizontally-scaled instance to serve subsequent requests
 * and read the session attributes, enabling stateless horizontal scaling
 * without sticky sessions or server affinity.
 *
 * <p>No changes to the session API calls are required – the Spring Session
 * {@code DelegatingFilterProxy} intercepts {@code req.getSession()} and
 * returns a Redis-backed {@link HttpSession} implementation automatically.
 */
@WebServlet("/doctorLogin")
public class DoctorLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		//get email and password which is coming from doctor_login.jsp page
		String email = req.getParameter("email");
		String password = req.getParameter("password");

		// HttpSession is transparently backed by Amazon ElastiCache for Redis
		// via Spring Session (springSessionRepositoryFilter in web.xml +
		// RedisSessionConfig). Session objects stored here are written to the
		// distributed Redis store, enabling stateless horizontal scaling.
		HttpSession session = req.getSession();

		//create DB connection
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());
		
		//call loginDoctor() method for doctor login which method declared in DoctorDAO 
		Doctor doctor = docDAO.loginDoctor(email, password);

		if (doctor != null) {
			//means doctor is valid or exist
			//then store particular logged in doctor object in session
			session.setAttribute("doctorObj", doctor);
			//and redirect the particular doctor index page which is reside doctor folder
			resp.sendRedirect("doctor/index.jsp");//doctor index means dashboard of doctors
		} else {
			session.setAttribute("errorMsg", "Invalid email or password");
			resp.sendRedirect("doctor_login.jsp");
		}

	}

}
