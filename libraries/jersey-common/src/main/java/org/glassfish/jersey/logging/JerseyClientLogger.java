package org.glassfish.jersey.logging;

import cd.connect.jersey.common.logging.JerseyFilteringConfiguration;

import javax.inject.Singleton;

@Singleton
public class JerseyClientLogger extends FilteringClientLoggingFilter {
	public JerseyClientLogger() {
		super(new JerseyFilteringConfiguration());
	}
}
