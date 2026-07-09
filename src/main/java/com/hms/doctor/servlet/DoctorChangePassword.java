package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession; // Managed by Spring Session Redis (Amazon ElastiCache for Redis)

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;

@WebServlet("/doctorChangePassword")
public class DoctorChangePassword extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int doctorId = Integer.parseInt(req.getParameter("doctorId"));
		String newPassword = req.getParameter("newPassword");
		String oldPassword = req.getParameter("oldPassword");

		DoctorDAO doctorDAO = new DoctorDAO(DBConnection.getConn());

		HttpSession session = req.getSession(); // Distributed session via Amazon ElastiCache for Redis

		if (doctorDAO.checkOldPassword(doctorId, oldPassword)) {

			if (doctorDAO.changePassword(doctorId, newPassword)) {
				
				session.setAttribute("successMsg", "Password change successfully."); // Stored in Amazon ElastiCache for Redis
				resp.sendRedirect("doctor/edit_profile.jsp");

			} else {
				
				session.setAttribute("errorMsg", "Something went wrong on server!"); // Stored in Amazon ElastiCache for Redis
				resp.sendRedirect("doctor/edit_profile.jsp");

			}

		} else {
			session.setAttribute("errorMsg", "Old Password not match"); // Stored in Amazon ElastiCache for Redis
			resp.sendRedirect("doctor/edit_profile.jsp");

		}
	}

}
