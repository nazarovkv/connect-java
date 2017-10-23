package cd.connect.tracing.jersey;

import io.opentracing.ActiveSpan;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

/**
 * If you are using the OpenTracing jaxrs contrib, you need this if you want existing ActiveSpan's to be honoured.
 *
 * It allows us to have an active span that was not generated from inside the jaxrs adapters.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Priority(Priorities.HEADER_DECORATOR-1)
public class ClientTracingFixFilter implements ClientRequestFilter {
	private final Tracer tracer;
	private static final String CHILD = "io.opentracing.contrib.jaxrs2.client.ClientTracingFilter.child_of";

	@Inject
	public ClientTracingFixFilter(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		ActiveSpan span = tracer.activeSpan();

		SpanContext parentSpanContext = SpanContext.class.cast(requestContext.getProperty(CHILD));

		if (span != null && parentSpanContext == null) {
			requestContext.setProperty(CHILD, span.context());
		}
	}
}
