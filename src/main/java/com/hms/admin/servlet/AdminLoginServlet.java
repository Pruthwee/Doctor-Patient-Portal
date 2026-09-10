package com.hms.admin.servlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.config.SecretsManagerUtil;
import com.hms.entity.User;

/**
 * Handles administrator login.
 *
 * <p>Admin credentials are no longer hardcoded in source code. They are
 * retrieved at runtime from <strong>AWS Secrets Manager</strong> via
 * {@link SecretsManagerUtil}, which reads the secret named by the environment
 * variable {@code HMS_SECRET_NAME} (default: {@code hms/app/credentials}).
 *
 * <p>Expected secret JSON structure:
 * <pre>
 * {
 *   "admin_email":    "admin@example.com",
 *   "admin_password": "s3cr3tP@ssw0rd"
 * }
 * </pre>
 *
 * <p><strong>Session storage (cr-java-0065):</strong> HTTP session state is no
 * longer stored in the servlet container's in-memory store. The
 * {@code springSessionRepositoryFilter} registered in {@code web.xml}
 * transparently replaces {@link HttpSession} with a Redis-backed session
 * managed by Spring Session and stored in <strong>Amazon ElastiCache for
 * Redis</strong>. This enables stateless, horizontally-scalable instances
 * with no server affinity or sticky-session requirements.
 */
@WebServlet("/adminLogin")
public class AdminLoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(AdminLoginServlet.class.getName());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			String email    = req.getParameter("email");
			String password = req.getParameter("password");

			// HttpSession is transparently backed by Amazon ElastiCache for Redis
			// via Spring Session (springSessionRepositoryFilter in web.xml +
			// RedisSessionConfig). No API change is required here; the session
			// data is stored in the distributed Redis store instead of in-memory.
			HttpSession session = req.getSession();

			// Retrieve admin credentials from AWS Secrets Manager at runtime.
			// This replaces the previously hardcoded values ("admin@gmail.com" / "admin")
			// and eliminates the security vulnerability of embedding credentials in source code.
			String adminEmail    = SecretsManagerUtil.getAdminEmail();
			String adminPassword = SecretsManagerUtil.getAdminPassword();

			if (adminEmail == null || adminPassword == null) {
				LOGGER.log(Level.SEVERE, "Admin credentials could not be retrieved from AWS Secrets Manager.");
				session.setAttribute("errorMsg", "Service configuration error. Please contact the administrator.");
				resp.sendRedirect("admin_login.jsp");
				return;
			}

			// Validate submitted credentials against the secrets-managed values.
			if (adminEmail.equals(email) && adminPassword.equals(password)) {
				// "adminObj" presence in session signals that an admin is logged in.
				// Stored in Redis via Spring Session – survives instance restarts.
				session.setAttribute("adminObj", new User());
				resp.sendRedirect("admin/index.jsp");
			} else {
				session.setAttribute("errorMsg", "Invalid Username or Password.");
				resp.sendRedirect("admin_login.jsp");
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Unexpected error during admin login", e);
			e.printStackTrace();
		}
	}
}
