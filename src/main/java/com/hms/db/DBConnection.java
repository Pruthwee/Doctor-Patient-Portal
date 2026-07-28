package com.hms.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

	private static Connection conn;
	private static JedisPool jedisPool;
	
	public static Connection getConn() {
		
		try {
			
			Class.forName("com.mysql.cj.jdbc.Driver"); // Service isolation enforced via K8s NetworkPolicy on EKS			
			//step:2- create a connection
			String dbUrl = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:mysql://localhost:3306/hospital";
			String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
			String dbPass = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "wasim";
			
			conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
			
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		
		return conn;
	}

	public static Jedis getRedisConnection() {
		if (jedisPool == null) {
			String redisHost = System.getenv("REDIS_HOST") != null ? System.getenv("REDIS_HOST") : "localhost";
			int redisPort = System.getenv("REDIS_PORT") != null ? Integer.parseInt(System.getenv("REDIS_PORT")) : 6379;
			String redisPass = System.getenv("REDIS_PASSWORD");
			
			JedisPoolConfig poolConfig = new JedisPoolConfig();
			if (redisPass != null && !redisPass.isEmpty()) {
				jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPass);
			} else {
				jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
			}
		}
		return jedisPool.getResource();
	}
}
