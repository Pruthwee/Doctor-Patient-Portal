package com.hms.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

	private static Connection conn;
	
	public static Connection getConn() {
		
		try {
			
			// Load the MySQL JDBC driver (com.mysql.cj.jdbc.Driver for mysql-connector-j 8.x)
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			// Create a connection to the database
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/hospital", "root", "wasim");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return conn;
	}
}
