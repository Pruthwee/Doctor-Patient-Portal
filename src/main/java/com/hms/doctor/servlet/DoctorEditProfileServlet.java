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

@WebServlet("/doctor_edit_profile")
public class DoctorEditProfileServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// get all data which is coming from doctor.jsp doctor details
			String fullName = req.getParameter("fullName");
			String dateOfBirth = req.getParameter("dateOfBirth");
			String qualification = req.getParameter("qualification");
			String specialist = req.getParameter("specialist");
			String email = req.getParameter("email");
			String phone = req.getParameter("phone");
			//String password = req.getParameter("password");

			
			int id = Integer.parseInt(req.getParameter("doctorId"));

			Doctor doctor = new Doctor(id, fullName, dateOfBirth, qualification, specialist, email, phone, "");

			DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

			boolean f = docDAO.editDoctorProfile(doctor);

			// Use JWT for stateless communication instead of server-side session for messages
			if (f == true) {
				// In a stateless architecture, we avoid storing the doctor object in session.
				// We can pass the ID in the redirect and let the JSP fetch it, or use a JWT.
				resp.sendRedirect("doctor/edit_profile.jsp?successMsgForD=Doctor update Successfully&doctorId=" + id);

			} else {
				resp.sendRedirect("doctor/edit_profile.jsp?errorMsgForD=Something went wrong on server!");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
