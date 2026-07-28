package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;
import com.hms.util.JwtUtil;

@WebServlet("/doctorLogin")
public class DoctorLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		//get email and password which is coming from doctor_login.jsp page
		String email = req.getParameter("email");
		String password = req.getParameter("password");

		//create DB connection
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());
		
		//call loginDoctor() method for doctor login which method declared in DoctorDAO 
		Doctor doctor = docDAO.loginDoctor(email, password);

		if (doctor != null) {
			//means doctor is valid or exist
			//Instead of session, generate a JWT token
			String token = JwtUtil.generateToken(doctor.getEmail());
			
			// In a real application, we would set this token in a cookie or return it in the response body
			// For this transformation, we'll pass it as a parameter to simulate statelessness
			resp.sendRedirect("doctor/index.jsp?token=" + token);
		} else {
			resp.sendRedirect("doctor_login.jsp?errorMsg=Invalid email or password");
		}

	}

}
