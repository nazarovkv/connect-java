package cd.connect.tracing.extractors;

import cd.connect.tracing.HeaderLoggingConfiguration;
import io.opentracing.SpanContext;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import java.util.UUID;

/**
 * If the tracer is the default (no-op) then use this. Ideally "request-id" should be set in the
 * context already, this just deals with nothing having received a request-id header.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class NoopTracingExtractor implements TracingExtractor {
	private final HeaderLoggingConfiguration headerLoggingConfiguration;

	@Inject
	public NoopTracingExtractor(HeaderLoggingConfiguration headerLoggingConfiguration) {
		this.headerLoggingConfiguration = headerLoggingConfiguration;
	}

	@Override
	public void embedActiveSpanContext(SpanContext spanContext, HeaderSource headerSource) {
		if (MDC.get(TracingExtractor.REQUEST_ID) == null) {
			MDC.put(TracingExtractor.REQUEST_ID, headerLoggingConfiguration.getAppName() + ":" + UUID.randomUUID().toString());
		}
	}
}
