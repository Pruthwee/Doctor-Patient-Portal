package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//Remove JWT cookie
		Cookie cookie = new Cookie("jwt", "");
		cookie.setMaxAge(0);
		cookie.setPath("/");
		resp.addCookie(cookie);
		
		resp.sendRedirect("admin_login.jsp");
		
		
		
	}

	
}
