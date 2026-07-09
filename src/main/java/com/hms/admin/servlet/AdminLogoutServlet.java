package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession; // Managed by Spring Session Redis (Amazon ElastiCache for Redis)

@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//get session means get "adminObj" and remove it, logout done!
		HttpSession session = req.getSession(); // Distributed session via Amazon ElastiCache for Redis
		session.removeAttribute("adminObj");
		//show message after logout
		session.setAttribute("successMsg", "Admin Logout Successfully"); // Stored in Amazon ElastiCache for Redis
		resp.sendRedirect("admin_login.jsp");
		
		
		
	}

	
}
