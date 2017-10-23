package cd.connect.tracing.extractors;

import io.opentracing.SpanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.core.MultivaluedMap;

/**
 * This is the implementation if Jaeger is included. It allows us to determine
 * what the trace/span/etc actually are.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class JaegerTracingExtractor implements TracingExtractor {
	public static boolean enabled = false;
	private static final Logger log = LoggerFactory.getLogger(JaegerTracingExtractor.class);

	public JaegerTracingExtractor() {
		enabled = true; // someone created me, so i now exist and am configured to be used
	}

	@Override
	public void embedActiveSpanContext(SpanContext spanContext, HeaderSource headerSource) {
		if (spanContext == null) {
			log.error("You have wired the {} but are using the NoopTracer!", getClass().getName());
		} else {
			com.uber.jaeger.SpanContext realSpan = com.uber.jaeger.SpanContext.class.cast(spanContext);

			MDC.put(REQUEST_ID, Long.toHexString(realSpan.getTraceId()));

			if (realSpan.getParentId() > 0) {
				MDC.put(REQUEST_PARENT_SPAN, Long.toHexString(realSpan.getParentId()));
			}

			MDC.put(REQUEST_SPAN, Long.toHexString(realSpan.getSpanId()));
		}
	}
}
