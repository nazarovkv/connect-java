package cd.connect.spring.jersey.log;

import cd.connect.context.ConnectContext;
import cd.connect.spring.jersey.JerseyLoggerPoint;
import net.stickycode.stereotype.configured.PostConfigured;
import org.glassfish.jersey.logging.Constants;
import org.glassfish.jersey.logging.FilteringClientLoggingFilter;
import org.glassfish.jersey.logging.FilteringServerLoggingFilter;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.servlet.ServletProperties;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configurable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Being here and in a component allows for it to be reloaded by the configuration system as necessary.
 *
 * Many of these things cannot be reloaded however. Once a ResourceConfig is loaded, it stays loaded. We may
 * support servlet un/reloading later.
 *
 * These are only GLOBAL settings for all jersey contexts. If you load multiple of them and you want individual
 * configuration you will need to do this another way.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */

public class JerseyFilteringConfiguration implements JerseyFiltering {
	private Set<String> excludeUri;
	private Set<String> excludeEntirelyUri;

	protected String exclude = ""; // e.g. /(apibrowser|metrics|service|status).*
	protected String tracing = ""; // e.g. ON_DEMAND
	protected Integer bufferSize;

	public JerseyFilteringConfiguration() {
		// we need these values on startup as we need this bean to be up and valid when the
		// wiring is being done.
		init();
	}

	@PostConfigured
	public void init() {

		exclude = System.getProperty("jersey.exclude", "");
		tracing = System.getProperty("jersey.tracing", "");

		bufferSize = Integer.parseInt(System.getProperty("jersey.bufferSize", "8192"));

		/*
		* we are default using Kubernetes and Prometheus, so we should ignore at least these by default.
		 */
		excludeUri = deconstructConfiguration(System.getProperty("jersey.logging.exclude-body-uris", ""));
		excludeEntirelyUri = deconstructConfiguration(System.getProperty("jersey.logging.exclude-entirely-uris", ""));
	}

	private Set<String> deconstructConfiguration(String toSplit)  {
		return Stream.of(toSplit.split(","))
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(s -> s.length() > 0)
			.collect(Collectors.toSet());
	}

	/**
	 * Return true if the body payload should not be logged.
	 */
	public boolean excludePayloadForUri(String uriPath) {
		if (excludeUri.contains(uriPath)) {
			if (JerseyLoggerPoint.logger.isTraceEnabled()) {
				// mention we are excluding payload logging
				ConnectContext.set(Constants.REST_CONTEXT, "exclude payload logging for uriPath:" + uriPath);
				JerseyLoggerPoint.logger.trace("no payload");
				ConnectContext.remove(Constants.REST_CONTEXT);
			}
			return true;
		}
		return false;
	}

	/**
	 * should we exclude this reference entirely?
	 */
	public boolean excludeForUri(String uriPath) {
		return excludeEntirelyUri.contains(uriPath);
	}

	@Override
	public void registerFilters(Configurable<?> resourceConfig) {
		if (exclude.length() > 0) {
			resourceConfig.property(ServletProperties.FILTER_STATIC_CONTENT_REGEX, exclude);
		}

		if (tracing.length() > 0) {
			resourceConfig.property("jersey.config.server.tracing.type", tracing);
		}

		if (resourceConfig.getConfiguration().getRuntimeType() == RuntimeType.CLIENT) {
			// determine if we need to log any of the Jersey stuff. See the TracingJerseyLogger for details.
			if (JerseyLoggerPoint.logger.isTraceEnabled()) {
				resourceConfig.register(newClientLogger(this, LoggingFeature.Verbosity.PAYLOAD_ANY));
			} else if (JerseyLoggerPoint.logger.isDebugEnabled()) {
				resourceConfig.register(newClientLogger(this, LoggingFeature.Verbosity.HEADERS_ONLY));
			}
		} else {
			// determine if we need to log any of the Jersey stuff. See the TracingJerseyLogger for details.
			if (JerseyLoggerPoint.logger.isTraceEnabled()) {
				resourceConfig.register(newServerLogger(this, LoggingFeature.Verbosity.PAYLOAD_ANY));
			} else if (JerseyLoggerPoint.logger.isDebugEnabled()) {
				resourceConfig.register(newServerLogger(this, LoggingFeature.Verbosity.HEADERS_ONLY));
			}
		}
	}

	private FilteringServerLoggingFilter newServerLogger(JerseyFiltering jerseyFiltering, LoggingFeature.Verbosity verbosity) {
		return new FilteringServerLoggingFilter(jerseyFiltering,  verbosity, bufferSize);
	}

	private FilteringClientLoggingFilter newClientLogger(JerseyFiltering jerseyFiltering, LoggingFeature.Verbosity verbosity) {
		return new FilteringClientLoggingFilter(jerseyFiltering, verbosity, bufferSize);
	}
}
