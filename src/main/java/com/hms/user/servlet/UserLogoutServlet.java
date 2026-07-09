package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession; // Managed by Spring Session Redis

@WebServlet("/userLogout")
public class UserLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		HttpSession session = req.getSession(); // Distributed session via Redis
		session.removeAttribute("userObj");
		session.setAttribute("successMsg", "User Logout Successfully."); // Stored in Redis
		resp.sendRedirect("user_login.jsp");
		
	}

	
	
}
