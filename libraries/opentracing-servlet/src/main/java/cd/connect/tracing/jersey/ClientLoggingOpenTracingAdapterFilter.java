package cd.connect.tracing.jersey;

import cd.connect.tracing.HeaderLoggingConfiguration;
import cd.connect.tracing.extractors.TracingExtractor;
import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import org.slf4j.MDC;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Map;

/**
 * This looks for any logging parameters on the MDC and adds them to the outgoing HTTP
 * headers. The ServerContextPassFilter picks it up on the other side.
 *
 * It relies on the premise that the jaxrs open tracing logging headers have already executed and provided
 * a trace-id.
 *
 * @author Richard Vowles - https://google.com/+RichardVowles
 */
@Priority(Priorities.HEADER_DECORATOR+1) // "lower than open tracing"
public class ClientLoggingOpenTracingAdapterFilter implements ClientRequestFilter {
  private final HeaderLoggingConfiguration headerLoggingConfiguration;
	private final Tracer tracer;
	private final TracingExtractor tracingExtractor;

	@Inject
	public ClientLoggingOpenTracingAdapterFilter(HeaderLoggingConfiguration headerLoggingConfiguration, Tracer tracer,
	                                             TracingExtractor tracingExtractor) {
    this.headerLoggingConfiguration = headerLoggingConfiguration;
		this.tracer = tracer;
		this.tracingExtractor = tracingExtractor;
	}

  @Override
  public void filter(ClientRequestContext context) throws IOException {
    final MultivaluedMap<String, Object> headers = context.getHeaders();
	  Map<String, String> validHeaders = headerLoggingConfiguration.getHeaderToLoggingMapping();

	  ActiveSpan activeSpan = tracer.activeSpan();

	  if (activeSpan != null) {
		  // ensure we can track, this is assuming we have a consistent id
		  tracingExtractor.embedActiveSpanContext(activeSpan.context(), key -> {
			  Object val = headers.getFirst(key);

			  return val == null ? null : val.toString();
		  });
	  }

	  for(String sendHeader : headerLoggingConfiguration.getAcceptHeaders()) {
    	String localName = validHeaders.get(sendHeader);

		  if ((activeSpan == null || activeSpan.getBaggageItem(localName) == null) && localName.equals(TracingExtractor.REQUEST_ID)) {
		  	String val = MDC.get(localName);

		  	if (val != null) {
		  		headers.add(sendHeader, val);
			  }
		  }
    }
  }
}
