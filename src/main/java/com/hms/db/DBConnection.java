package com.hms.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

	private static Connection conn;
	
	public static Connection getConn() {
		
		try {
			
			//step:1 for connection - load the driver class 
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			//step:2- create a connection
			String host = System.getenv("DB_HOST");
			if (host == null || host.isEmpty()) {
				host = "localhost";
			}
			String port = System.getenv("DB_PORT");
			if (port == null || port.isEmpty()) {
				port = "3306";
			}
			String database = System.getenv("DB_NAME");
			if (database == null || database.isEmpty()) {
				database = "hospital";
			}
			String user = System.getenv("DB_USER");
			if (user == null) {
				user = "";
			}
			String password = System.getenv("DB_PASSWORD");
			if (password == null) {
				password = "";
			}
			String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
			conn = DriverManager.getConnection(url, user, password);
			
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		
		return conn;
	}
}
