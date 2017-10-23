package cd.connect.spring.jersey;

import cd.connect.spring.Module;
import cd.connect.spring.jersey.log.JerseyFilteringConfiguration;

import java.util.stream.Stream;

/**
 * Must include this in Spring context to register filtering capability.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class JerseyConfig extends Module {
	@Override
	public void register() {
		register(Stream.of(JerseyFilteringConfiguration.class));
	}
}
