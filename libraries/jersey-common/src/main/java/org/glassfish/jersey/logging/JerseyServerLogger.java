package org.glassfish.jersey.logging;

import cd.connect.jersey.common.logging.JerseyFilteringConfiguration;

public class JerseyServerLogger extends FilteringServerLoggingFilter {
	public JerseyServerLogger() {
		super(new JerseyFilteringConfiguration());
	}
}
