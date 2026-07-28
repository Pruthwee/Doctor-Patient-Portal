package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import com.hms.entity.User;
import com.hms.util.JwtUtil;

@WebServlet("/adminLogin")
public class AdminLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try {
			
			//create one static Admin for this project
			String email = req.getParameter("email");
			String password = req.getParameter("password");
			
			//logic for a static Admin
			if ("admin@gmail.com".equals(email) && "admin".equals(password)) {
				
				String token = JwtUtil.generateToken(email);
				Cookie cookie = new Cookie("jwt", token);
				cookie.setHttpOnly(true);
				resp.addCookie(cookie);
				
				resp.sendRedirect("admin/index.jsp");
			}
			else {
				resp.sendRedirect("admin_login.jsp");
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
}
