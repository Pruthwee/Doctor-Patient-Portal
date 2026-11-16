package com.hms.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

	private static Connection conn;
	
	public static Connection getConn() {
		
		try {
			
			//step:1- create a connection
			//JDBC 4.0+ drivers auto-register via ServiceLoader, no Class.forName needed
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/hospital","root","wasim");
			
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		
		return conn;
	}
}
