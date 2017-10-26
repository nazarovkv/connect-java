package cd.connect.tracing.extractors;

import io.opentracing.SpanContext;
import org.slf4j.MDC;

/**
 * This is taken from https://istio.io/v-0.1/docs/tasks/zipkin-tracing.html
 *
 * We ignore request-id because of : http://zipkin.io/pages/instrumenting.html
 *
 * In this case Istio has sideloaded Zipkin and will generate the incoming and outgoing
 * spans before we see it. It is only our job to propagate the headers.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class ZipkinIstioTracingExtractor implements TracingExtractor {

	private void setLog(String header, String logKey, HeaderSource headers) {
		String val = headers.getHeader(header);
		if (val != null) {
			MDC.put(logKey, val);
		}
	}

	@Override
	public void embedActiveSpanContext(SpanContext spanContext, HeaderSource headerSource) {
		setLog("x-b3-traceid", TracingExtractor.REQUEST_ID, headerSource);
		setLog("x-b3-spanid", TracingExtractor.REQUEST_SPAN, headerSource);
		setLog("x-b3-parentspanid", TracingExtractor.REQUEST_PARENT_SPAN, headerSource);
	}

	@Override
	public boolean sendRequestHeader(String localName) {
		return false;
	}
}
