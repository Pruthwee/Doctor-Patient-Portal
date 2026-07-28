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

@WebServlet("/user_register")
public class UserRegisterServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			// get all data/value which is coming from signup.jsp page for new User registration
			String fullName = req.getParameter("fullName");
			String email = req.getParameter("email");
			String password = req.getParameter("password");

			// Set all data to User Entity
			User user = new User(fullName, email, password);

			// Create Connection with DB
			UserDAO userDAO = new UserDAO(DBConnection.getConn());

			if (userDAO.registerUser(user)) {
				resp.sendRedirect("signup.jsp?successMsg=Registration successful!");
			} else {
				resp.sendRedirect("signup.jsp?errorMsg=Something went wrong!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			resp.sendRedirect("signup.jsp?errorMsg=An error occurred during registration.");
		}
	}
}
