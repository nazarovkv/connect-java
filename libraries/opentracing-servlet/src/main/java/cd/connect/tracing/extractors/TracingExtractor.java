package cd.connect.tracing.extractors;

import io.opentracing.SpanContext;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface TracingExtractor {
	String REQUEST_ID = "request-id";
	String REQUEST_SPAN = "ot.span";
	String REQUEST_PARENT_SPAN = "ot.parent";

	interface HeaderSource {
		String getHeader(String key);
	}

	void embedActiveSpanContext(SpanContext spanContext, HeaderSource headerSource);
}
