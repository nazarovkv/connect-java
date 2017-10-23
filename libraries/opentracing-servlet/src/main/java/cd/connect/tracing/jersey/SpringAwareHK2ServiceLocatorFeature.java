package cd.connect.tracing.jersey;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.ServiceLocatorProvider;
import org.jvnet.hk2.spring.bridge.api.SpringBridge;
import org.jvnet.hk2.spring.bridge.api.SpringIntoHK2Bridge;
import org.springframework.context.ApplicationContext;

import javax.inject.Provider;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Integrates the default Jersey HK2 ServiceLocator with Spring via the
 * Spring/HK2 bridge from Oracle.
 *
 * @author Jacek Furmankewicz, Richard Vowles
 */
public class SpringAwareHK2ServiceLocatorFeature implements Feature, Provider<ServiceLocator> {

	private ApplicationContext springContext;
	private ServiceLocator locator;

	public SpringAwareHK2ServiceLocatorFeature(ApplicationContext springContext) {
		this.springContext = springContext;
	}

	@Override
	public ServiceLocator get() {
		if (locator == null) {
			throw new RuntimeException("Service locator is not available yet.");
		}

		return locator;
	}

	@Override
	public boolean configure(FeatureContext context) {
		locator =  ServiceLocatorProvider.getServiceLocator(context);

		SpringBridge.getSpringBridge().initializeSpringBridge(locator);
		SpringIntoHK2Bridge springBridge = locator.getService(SpringIntoHK2Bridge.class);
		springBridge.bridgeSpringBeanFactory(springContext.getAutowireCapableBeanFactory());

		return true;
	}
}
