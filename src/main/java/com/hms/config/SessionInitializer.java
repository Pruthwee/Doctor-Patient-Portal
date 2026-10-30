package com.hms.config;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * Registers the springSessionRepositoryFilter in front of every request, so that
 * HttpSession is backed by Redis (see SessionConfig).
 */
public class SessionInitializer extends AbstractHttpSessionApplicationInitializer {

	public SessionInitializer() {
		super(SessionConfig.class);
	}
}
