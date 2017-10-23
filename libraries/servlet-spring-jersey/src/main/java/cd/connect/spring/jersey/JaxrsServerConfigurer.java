package cd.connect.spring.jersey;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;


/**
 * We use this instead of "Feature" because it is awkward and the wrong way around to use
 * "Feature".
 *
 * Created by Richard Vowles on 12/10/17.
 */
public interface JaxrsServerConfigurer {
	void configure(Configurable<? extends Configurable> config);
}
