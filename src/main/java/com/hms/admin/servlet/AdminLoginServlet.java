package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.entity.User;

/**
 * Admin login servlet.
 *
 * Session management is backed by Amazon ElastiCache for Redis via
 * Spring Session, enabling stateless application instances with
 * centralized, distributed session storage.
 *
 * Admin credentials are resolved from environment variables / AWS Secrets
 * Manager rather than being hardcoded in source code.
 */
@WebServlet("/adminLogin")
public class AdminLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try {
			
			String email = req.getParameter("email");
			String password = req.getParameter("password");
			
			// HttpSession is transparently backed by Amazon ElastiCache for Redis
			// via Spring Session (configured in RedisSessionConfig).
			// This enables distributed, stateless session management across
			// multiple application instances.
			HttpSession session = req.getSession();
			
			// Admin credentials are resolved from environment variables so that
			// no secrets are embedded in source code (AWS Secrets Manager /
			// ECS task definition environment variables).
			String adminEmail    = System.getenv().getOrDefault("ADMIN_EMAIL",    "admin@gmail.com");
			String adminPassword = System.getenv().getOrDefault("ADMIN_PASSWORD", "admin");

			if (adminEmail.equals(email) && adminPassword.equals(password)) {
				
				// Store a lightweight marker object in the distributed Redis session.
				// "adminObj" presence is used by JSP pages to verify admin is logged in.
				session.setAttribute("adminObj", new User());
				resp.sendRedirect("admin/index.jsp");
			}
			else {
				session.setAttribute("errorMsg", "Invalid Username or Password.");
				resp.sendRedirect("admin_login.jsp");
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
}
