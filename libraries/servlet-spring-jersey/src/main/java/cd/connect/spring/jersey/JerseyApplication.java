package cd.connect.spring.jersey;

import org.springframework.context.ApplicationContext;

import java.util.stream.Stream;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface JerseyApplication {
	/**
	 * This indicates where in the Application properties the list of interfaces that
	 * were registered (if any) are stored. This allows us to not throw them away and use
	 * them later if required.
	 */
	public static final String RESOURCE_INTERFACE_LIST = "connect.resource.service";

	void init(ApplicationContext context, Stream<Class<?>> resources);
}
