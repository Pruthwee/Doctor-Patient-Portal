package com.hms.entity;

/**
 * User entity representing a registered patient/user in the HMS system.
 * Password field is excluded from toString() to prevent accidental
 * credential exposure in logs (AWS CloudWatch) or error messages.
 */
public class User {
	private int id;
	private String fullName;
	private String email;
	private String password;
	
	
	public User() {
		super();
		// TODO Auto-generated constructor stub
	}


	public User(int id, String fullName, String email, String password) {
		super();
		this.id = id;
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}


	public User(String fullName, String email, String password) {
		super();
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getFullName() {
		return fullName;
	}


	public void setFullName(String fullName) {
		this.fullName = fullName;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	/**
	 * Password is intentionally excluded from toString() to prevent
	 * credential leakage in application logs (e.g., AWS CloudWatch Logs).
	 * Credentials must be managed via AWS Secrets Manager.
	 */
	@Override
	public String toString() {
		return "User [id=" + id + ", fullName=" + fullName + ", email=" + email + ", password=***REDACTED***]";
	}
	
	
	
	
	
	
	
	
}
