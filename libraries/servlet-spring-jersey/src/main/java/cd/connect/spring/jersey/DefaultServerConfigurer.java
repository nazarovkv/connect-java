package cd.connect.spring.jersey;

import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.glassfish.jersey.server.wadl.internal.WadlResource;

import javax.ws.rs.core.Configurable;

import static org.glassfish.jersey.servlet.ServletProperties.PROVIDER_WEB_APP;

/**
 * Created by Richard Vowles on 12/10/17.
 */
public class DefaultServerConfigurer implements JaxrsServerConfigurer {
	@Override
	public void configure(Configurable<? extends Configurable> config) {
		config.property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
		config.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
		config.property(CommonProperties.MOXY_JSON_FEATURE_DISABLE, true);

		config.property(PROVIDER_WEB_APP, false); // do not scan!

		config.register(RequestContextFilter.class);
		config.register(JacksonFeature.class);
		config.register(MultiPartFeature.class);
		config.register(GZipEncoder.class);
		config.register(JacksonContextProvider.class);

		// allow generation of WADLs.
		config.register(WadlResource.class);

		// support swagger requests
		config.register(ConnectSwaggerApiResource.class);
		config.register(SwaggerSerializers.class);
	}
}
