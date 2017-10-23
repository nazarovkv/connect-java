package cd.connect.spring.jersey;

import org.springframework.context.ApplicationContext;

import java.util.stream.Stream;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface JerseyApplication {
	void init(ApplicationContext context, Stream<Class<?>> resources);
}
