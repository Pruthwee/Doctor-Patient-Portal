package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/userLogout")
public class UserLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// In a stateless JWT architecture, logout is typically handled by the client 
		// deleting the token. On the server side, we just redirect.
		resp.sendRedirect("user_login.jsp?successMsg=User Logout Successfully.");
		
	}

	
	
}
