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
			String dbHost = System.getenv().getOrDefault("DB_HOST", "localhost");
			String dbPort = System.getenv().getOrDefault("DB_PORT", "3306");
			String dbName = System.getenv().getOrDefault("DB_NAME", "hospital");
			String dbUser = System.getenv().getOrDefault("DB_USER", "root");
			String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "");
			String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
			conn = DriverManager.getConnection(url, dbUser, dbPassword);
			
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		
		return conn;
	}
}
