package cd.connect.tracing.jersey;

import cd.connect.tracing.HeaderLoggingConfiguration;
import cd.connect.tracing.extractors.TracingExtractor;
import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Map;

/**
 * Look for a header value called "loggingHeaders" and if it exists, grab all of the values and push them
 * into the logging context
 *
 * @author Richard Vowles - https://google.com/+RichardVowles
 */
@Priority(Priorities.HEADER_DECORATOR+1) // "lower than open tracing"
public class ServerLoggingOpenTracingAdapterFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private final ThreadLocal<Long> processingTimeMs = new ThreadLocal<>();
  private Logger log = LoggerFactory.getLogger(getClass());
  private final HeaderLoggingConfiguration headerLoggingConfiguration;
	private final Tracer tracer;
	private final TracingExtractor tracingExtractor;

	@Inject
	public ServerLoggingOpenTracingAdapterFilter(HeaderLoggingConfiguration headerLoggingConfiguration, Tracer tracer,
	                                             TracingExtractor tracingExtractor) {
    this.headerLoggingConfiguration = headerLoggingConfiguration;
		this.tracer = tracer;
		this.tracingExtractor = tracingExtractor;
	}

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    MultivaluedMap<String, String> headers = requestContext.getHeaders();

    // copy valid headers from baggage and headers
	  ActiveSpan activeSpan = tracer.activeSpan();

    Map<String, String> validHeaders = headerLoggingConfiguration.getHeaderToLoggingMapping();
	  for (String acceptHeader : headerLoggingConfiguration.getAcceptHeaders()) {
		  String baggage =  null;

		  if (activeSpan != null) {
		  	String localName = validHeaders.get(acceptHeader);

			  baggage = activeSpan.getBaggageItem(localName);

			  if (baggage != null) {
				  MDC.put(localName, baggage);
			  }
		  }

		  if (baggage == null) {
		  	baggage = headers.getFirst(acceptHeader);

		  	if (baggage != null) {
		  		MDC.put(validHeaders.get(acceptHeader), baggage);
			  }
		  }
	  }

	  // store the application's name
	  String appName = headerLoggingConfiguration.getAppName();
	  if (appName != null) {
	  	MDC.put("appName", appName);

	  	if (activeSpan != null) {
	  		activeSpan.setTag("appName", appName);
		  }
	  }

	  tracingExtractor.embedActiveSpanContext(activeSpan == null ? null : activeSpan.context(), headers::getFirst);

    // if we don't have a id, add one
    processingTimeMs.set(System.currentTimeMillis());
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    try {
      Long start = processingTimeMs.get();

      int end = (start == null) ? 0 : (int)(System.currentTimeMillis() - start);

      LoggingContextResponse.toJsonLog(responseContext.getStatus(), end);

      if (responseContext.getStatus() >= 500) {
        log.error("request-complete");
      } else {
        log.debug("request-complete");
      }
    } finally {
      MDC.clear(); // should we just clean our own? i think we should clean everything
      processingTimeMs.remove();
    }
  }
}
