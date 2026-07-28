package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.User;
import com.hms.util.JwtUtil;

@WebServlet("/userLogin")
public class UserLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String email = req.getParameter("email");
		String password = req.getParameter("password");
		
		UserDAO userDAO = new UserDAO(DBConnection.getConn());
		User user = userDAO.loginUser(email, password);
		
		if (user!=null) {
			String token = JwtUtil.generateToken(user.getEmail());
			
			// In a real application, the token would be sent in a cookie or response header
			// For this transformation, we simulate by passing it as a parameter or just redirecting
			resp.sendRedirect("index.jsp?token=" + token); 
		}
		else {
			resp.sendRedirect("user_login.jsp?errorMsg=Invalid email or password"); 
		}
	}
	
	
}
