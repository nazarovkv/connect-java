package cd.connect.spring.jersey;

import cd.connect.spring.jersey.log.JerseyFiltering;

import javax.inject.Inject;
import javax.ws.rs.core.Configurable;

/**
 * Created by Richard Vowles on 12/10/17.
 */
public class FilteringServerConfigurer implements JaxrsServerConfigurer {
	private final JerseyFiltering jerseyFiltering;

	@Inject
	public FilteringServerConfigurer(JerseyFiltering jerseyFiltering) {
		this.jerseyFiltering = jerseyFiltering;
	}

	@Override
	public void configure(Configurable<? extends Configurable> config) {
		jerseyFiltering.registerFilters(config);
	}
}
