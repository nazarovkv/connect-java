package cd.connect.spring.jersey.log;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.Configurable;
import java.util.function.Consumer;

/**
 * This class must be usable as soon as it is injected. Typically the payload exclusions
 * can change at runtime, but the filter definitions cannot.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface JerseyFiltering {
	boolean excludePayloadForUri(String uriPath);
	boolean excludeForUri(String uriPath);

	void registerFilters(Configurable<?> resourceConfig);
}
