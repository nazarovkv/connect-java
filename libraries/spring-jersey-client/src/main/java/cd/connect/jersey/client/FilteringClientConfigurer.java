package cd.connect.jersey.client;

import cd.connect.spring.jersey.log.JerseyFiltering;

import javax.inject.Inject;
import javax.ws.rs.core.Configurable;

/**
 * Created by Richard Vowles on 12/10/17.
 */
public class FilteringClientConfigurer implements JaxrsClientConfigurer {
	private final JerseyFiltering filtering;

	@Inject
	public FilteringClientConfigurer(JerseyFiltering filtering) {
		this.filtering = filtering;
	}

	@Override
	public void configure(Configurable<? extends Configurable> config) {
		filtering.registerFilters(config);
	}
}
