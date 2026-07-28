package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;

@WebServlet("/update_doctor")
public class UpdateDoctorServlet extends HttpServlet {

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
			String password = req.getParameter("password");
			
			//here need to get id also...for updating the doctor details
			//doctors will update based on respective doctor's id
			int id = Integer.parseInt(req.getParameter("id"));

			Doctor doctor = new Doctor(id, fullName, dateOfBirth, qualification, specialist, email, phone, password);

			DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

			boolean f = docDAO.updateDoctor(doctor);

			if (f == true) {
				resp.sendRedirect("admin/view_doctor.jsp?successMsg=Doctor update Successfully");

			} else {
				resp.sendRedirect("admin/view_doctor.jsp?errorMsg=Something went wrong on server!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			resp.sendRedirect("admin/view_doctor.jsp?errorMsg=An error occurred while updating doctor details.");
		}
	}
}
