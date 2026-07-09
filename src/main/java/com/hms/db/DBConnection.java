package com.hms.db;

import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DBConnection {

	private static HikariDataSource dataSource;
	
	static {
		try {
			HikariConfig config = new HikariConfig();
			
			// Use AWS Secrets Manager or Environment Variables for cloud readiness
			String dbUrl = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:mysql://localhost:3306/hospital";
			String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
			String dbPass = System.getenv("DB_PASSWORD");
			
			config.setJdbcUrl(dbUrl);
			config.setUsername(dbUser);
			config.setPassword(dbPass);
			
			// Set connection and validation timeouts for cloud readiness
			config.setConnectionTimeout(30000);
			config.setValidationTimeout(5000);
			
			// Set socket and network timeouts for cloud readiness via dataSource properties
			config.addDataSourceProperty("socketTimeout", "30000");
			config.addDataSourceProperty("connectTimeout", "30000");
			
			// Recommended for MySQL
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			
			dataSource = new HikariDataSource(config);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error initializing HikariCP connection pool", e);
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
