package com.hms.db;

import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class DBConnection {

	private static HikariDataSource dataSource;
	
	static {
		try {
			HikariConfig config = new HikariConfig();
			
			// Use environment variables for database configuration to ensure cloud readiness
			String dbUrl = System.getenv("DB_URL");
			if (dbUrl == null) {
				dbUrl = "jdbc:mysql://localhost:3306/hospital"; // Fallback for local development
			}
			
			String dbUser = System.getenv("DB_USER");
			if (dbUser == null) {
				dbUser = "root"; // Fallback for local development
			}
			
			String dbPassword = System.getenv("DB_PASSWORD");
			if (dbPassword == null) {
				dbPassword = "wasim"; // Fallback for local development
			}
			
			// Set socket timeout to prevent indefinite hangs in cloud environments
			// This is passed as a property to the JDBC driver
			config.addDataSourceProperty("socketTimeout", "30000"); 
			dataSource = new HikariDataSource(config);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Connection getConn() {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
}
