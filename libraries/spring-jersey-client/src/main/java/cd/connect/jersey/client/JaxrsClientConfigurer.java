package cd.connect.jersey.client;

import javax.ws.rs.core.Configurable;

/**
 *  * We use this instead of "Feature" because it is awkward and the wrong way around to use
 * "Feature".
 * Created by Richard Vowles on 12/10/17.
 */
public interface JaxrsClientConfigurer {
	void configure(Configurable<? extends Configurable> config);
}
