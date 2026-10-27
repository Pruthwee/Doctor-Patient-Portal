package com.hms;

import java.io.File;
import java.nio.file.Files;

import javax.servlet.Filter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.jasper.servlet.JasperInitializer;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import com.hms.admin.servlet.AdminLoginServlet;
import com.hms.admin.servlet.AdminLogoutServlet;
import com.hms.admin.servlet.DeleteDoctorServlet;
import com.hms.admin.servlet.DoctorServlet;
import com.hms.admin.servlet.SpecialistServlet;
import com.hms.admin.servlet.UpdateDoctorServlet;
import com.hms.config.RedisSessionConfig;
import com.hms.config.SecretsHelper;
import com.hms.db.ConnectionReleaseFilter;
import com.hms.db.DBConnection;
import com.hms.doctor.servlet.DoctorChangePassword;
import com.hms.doctor.servlet.DoctorEditProfileServlet;
import com.hms.doctor.servlet.DoctorLoginServlet;
import com.hms.doctor.servlet.DoctorLogoutServlet;
import com.hms.doctor.servlet.UpdateStatus;
import com.hms.user.servlet.AppointmentServlet;
import com.hms.user.servlet.ChangePasswordServlet;
import com.hms.user.servlet.MyNewServlet;
import com.hms.user.servlet.UserLoginServlet;
import com.hms.user.servlet.UserLogoutServlet;
import com.hms.user.servlet.UserRegisterServlet;

/**
 * Starts the application as an executable JAR with an embedded Tomcat (it was a WAR before).
 * The port is read from PORT (default 8080).
 */
public class Application {

	private static final int DEFAULT_PORT = 8080;
	private static final int CONNECTION_TIMEOUT_MS = 20000;
	private static final int SESSION_TIMEOUT_MINUTES = 30;

	@SuppressWarnings("unchecked")
	private static final Class<? extends HttpServlet>[] SERVLETS = new Class[] { AdminLoginServlet.class,
			AdminLogoutServlet.class, DeleteDoctorServlet.class, DoctorServlet.class, SpecialistServlet.class,
			UpdateDoctorServlet.class, DoctorChangePassword.class, DoctorEditProfileServlet.class,
			DoctorLoginServlet.class, DoctorLogoutServlet.class, UpdateStatus.class, AppointmentServlet.class,
			ChangePasswordServlet.class, UserLoginServlet.class, UserLogoutServlet.class,
			UserRegisterServlet.class };

	public static void main(String[] args) throws Exception {

		String portValue = System.getenv("PORT");
		int port = (portValue == null || portValue.trim().isEmpty()) ? DEFAULT_PORT : Integer.parseInt(portValue.trim());

		File baseDir = Files.createTempDirectory("hms-tomcat").toFile();
		baseDir.deleteOnExit();

		Tomcat tomcat = new Tomcat();
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		tomcat.setPort(port);
		Connector connector = tomcat.getConnector();
		connector.setProperty("connectionTimeout", String.valueOf(CONNECTION_TIMEOUT_MS));

		Context ctx = tomcat.addContext("", baseDir.getAbsolutePath());
		ctx.setSessionTimeout(SESSION_TIMEOUT_MINUTES);
		ctx.addWelcomeFile("index.jsp");

		// JSPs and static files are packaged in META-INF/resources of the JAR
		WebResourceRoot resources = new StandardRoot(ctx);
		File location = new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		if (location.isFile()) {
			resources.createWebResourceSet(WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/",
					location.getAbsolutePath(), null, "/META-INF/resources");
		} else {
			resources.createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, "/", location.getAbsolutePath(),
					null, "/META-INF/resources");
		}
		ctx.setResources(resources);

		// session state in ElastiCache for Redis (when configured), then the per-request connection release
		Filter sessionFilter = RedisSessionConfig.createSessionFilter();
		if (sessionFilter != null) {
			addFilter(ctx, "springSessionRepositoryFilter", sessionFilter);
		}
		addFilter(ctx, "connectionReleaseFilter", new ConnectionReleaseFilter());

		// JSP support
		ctx.addServletContainerInitializer(new JasperInitializer(), null);
		Wrapper jsp = Tomcat.addServlet(ctx, "jsp", "org.apache.jasper.servlet.JspServlet");
		jsp.addInitParameter("fork", "false");
		jsp.setLoadOnStartup(3);
		ctx.addServletMappingDecoded("*.jsp", "jsp");
		ctx.addServletMappingDecoded("*.jspx", "jsp");

		// static files
		Wrapper defaultServlet = Tomcat.addServlet(ctx, "default", new DefaultServlet());
		defaultServlet.setLoadOnStartup(1);
		ctx.addServletMappingDecoded("/", "default");

		// the application's servlets, at the addresses given by their @WebServlet annotations
		for (Class<? extends HttpServlet> servletClass : SERVLETS) {
			WebServlet mapping = servletClass.getAnnotation(WebServlet.class);
			Tomcat.addServlet(ctx, servletClass.getSimpleName(), servletClass.newInstance());
			for (String pattern : mapping.value()) {
				ctx.addServletMappingDecoded(pattern, servletClass.getSimpleName());
			}
		}
		// declared in WEB-INF/web.xml of the former WAR
		Tomcat.addServlet(ctx, MyNewServlet.class.getSimpleName(), new MyNewServlet());
		ctx.addServletMappingDecoded("/MyNewServletRegisterUser", MyNewServlet.class.getSimpleName());

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				DBConnection.shutdown();
				SecretsHelper.close();
				RedisSessionConfig.shutdown();
			}
		});

		tomcat.start();
		tomcat.getServer().await();
	}

	private static void addFilter(Context ctx, String name, Filter filter) {
		FilterDef def = new FilterDef();
		def.setFilterName(name);
		def.setFilter(filter);
		ctx.addFilterDef(def);
		FilterMap map = new FilterMap();
		map.setFilterName(name);
		map.addURLPattern("/*");
		ctx.addFilterMap(map);
	}
}
