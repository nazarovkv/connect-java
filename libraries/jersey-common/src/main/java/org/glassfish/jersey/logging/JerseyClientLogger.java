package org.glassfish.jersey.logging;

import cd.connect.jersey.common.logging.JerseyFilteringConfiguration;

public class JerseyClientLogger extends FilteringClientLoggingFilter {
	public JerseyClientLogger() {
		super(new JerseyFilteringConfiguration());
	}
}
