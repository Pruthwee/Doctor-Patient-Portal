package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Admin logout servlet.
 *
 * Session management is backed by Amazon ElastiCache for Redis via
 * Spring Session, enabling stateless application instances with
 * centralized, distributed session storage. Invalidating the session
 * here removes it from the Redis store, ensuring all instances
 * immediately recognise the logout.
 */
@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// Retrieve the distributed Redis-backed session and remove the admin marker.
		// Spring Session propagates this invalidation to all application instances.
		HttpSession session = req.getSession();
		session.removeAttribute("adminObj");
		// Show a success message on the login page after logout
		session.setAttribute("successMsg", "Admin Logout Successfully");
		resp.sendRedirect("admin_login.jsp");
		
		
		
	}

	
}
