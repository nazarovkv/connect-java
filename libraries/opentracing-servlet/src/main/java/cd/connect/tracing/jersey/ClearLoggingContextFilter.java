package cd.connect.tracing.jersey;

import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import java.io.IOException;

/**
 * This runs before everything else to ensure we clear out the logging
 * context of any rubbish. Use it if you aren't specifically cleaning up the MDC context
 * after each block of code that puts something in.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@PreMatching
public class ClearLoggingContextFilter implements ContainerRequestFilter {
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		MDC.clear();
	}
}
