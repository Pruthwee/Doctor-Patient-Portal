package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;

@WebServlet("/updateStatus")
public class UpdateStatus extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try {
			
		 int 	id = Integer.parseInt(req.getParameter("id"));
		 int 	doctorId = Integer.parseInt(req.getParameter("doctorId"));
		 String comment = req.getParameter("comment");
		 
		 AppointmentDAO appDAO = new AppointmentDAO(DBConnection.getConn());
		 boolean f = appDAO.updateDrAppointmentCommentStatus(id, doctorId, comment);
		 
		 
		 if(f == true) {
			 resp.sendRedirect("doctor/patient.jsp?successMsg=Comment updated");
			 
		 }else {
			 resp.sendRedirect("doctor/patient.jsp?errorMsg=Something went wrong on server!");
		 }
		 
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
}
