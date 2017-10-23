package cd.connect.jersey.client;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.internal.FormProvider;

import javax.ws.rs.core.Configurable;

/**
 * Created by Richard Vowles on 12/10/17.
 */
public class DefaultClientConfigurer implements JaxrsClientConfigurer {
	@Override
	public void configure(Configurable<? extends Configurable> client) {
		client.property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
		client.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
		client.property(CommonProperties.JSON_PROCESSING_FEATURE_DISABLE, true);
		client.property(CommonProperties.MOXY_JSON_FEATURE_DISABLE, true);
		client.register(JacksonFeature.class);
		client.register(MultiPartFeature.class);
		client.register(FormProvider.class);
	}
}
