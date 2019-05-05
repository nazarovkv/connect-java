package cd.connect.jersey.common;

import org.glassfish.jersey.logging.JerseyClientLogger;
import org.glassfish.jersey.logging.JerseyServerLogger;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class LoggingConfiguration implements Feature {

	@Override
	public boolean configure(FeatureContext ctx) {
		ctx.register(JerseyServerLogger.class);
		ctx.register(JerseyClientLogger.class);
		return true;
	}
}
