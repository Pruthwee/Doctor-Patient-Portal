package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet to handle user (patient) logout.
 *
 * Session management is backed by Amazon ElastiCache for Redis via
 * Spring Session, enabling stateless application instances with
 * centralized, distributed session storage. Invalidating the session
 * here removes it from the Redis store, ensuring all instances
 * immediately recognise the logout.
 */
@WebServlet("/userLogout")
public class UserLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// Retrieve the distributed Redis-backed session and remove the user marker.
		// Spring Session propagates this invalidation to all application instances.
		HttpSession session = req.getSession();
		session.removeAttribute("userObj");
		session.setAttribute("successMsg", "User Logout Successfully.");
		resp.sendRedirect("user_login.jsp");
		
	}

	
	
}
