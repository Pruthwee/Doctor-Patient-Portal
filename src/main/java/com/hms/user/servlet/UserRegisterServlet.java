package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.User;

/**
 * Servlet to handle new user (patient) registration.
 *
 * Session management is backed by Amazon ElastiCache for Redis via
 * Spring Session, enabling stateless application instances with
 * centralized, distributed session storage.
 */
@WebServlet("/user_register")
public class UserRegisterServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// Get all data/value coming from signup.jsp page for new User registration
			String fullName = req.getParameter("fullName");
			String email = req.getParameter("email");
			String password = req.getParameter("password");

			// Set all data to User Entity
			User user = new User(fullName, email, password);

			// Create Connection with DB via HikariCP pool
			UserDAO userDAO = new UserDAO(DBConnection.getConn());
			
			// HttpSession is transparently backed by Amazon ElastiCache for Redis
			// via Spring Session for distributed, stateless session management.
			HttpSession session = req.getSession();
			

			// Call userRegister() and pass user object to insert/save user into DB.
			boolean f = userDAO.userRegister(user); // userRegister() method returns boolean

			if (f == true) {

				session.setAttribute("successMsg", "Register Successfully");
				resp.sendRedirect("signup.jsp"); // which page you want to show this msg

			} else {
				
				session.setAttribute("errorMsg", "Something went wrong!");
				resp.sendRedirect("signup.jsp"); // which page you want to show this msg
				
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
