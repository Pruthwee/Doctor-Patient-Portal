package com.hms.db;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Gives the pooled connection that DBConnection.getConn() handed out during a request back to
 * the pool when the request is finished.
 */
public class ConnectionReleaseFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try {
			chain.doFilter(request, response);
		} finally {
			DBConnection.releaseConn();
		}
	}
}
