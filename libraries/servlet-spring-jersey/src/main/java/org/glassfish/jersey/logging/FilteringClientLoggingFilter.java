package org.glassfish.jersey.logging;

import cd.connect.context.ConnectContext;
import cd.connect.spring.jersey.log.JerseyFiltering;
import org.glassfish.jersey.message.MessageUtils;

import javax.annotation.Priority;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.PreMatching;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Note Jersey's ClientLoggingFilter is final so this is effectively the replacement for that.
 * <p>
 * This takes into account excluding payload logging on special uri's that contain sensitive content.
 * </p>
 */
@ConstrainedTo(RuntimeType.CLIENT)
@Priority(Integer.MAX_VALUE)
@SuppressWarnings("ClassWithMultipleLoggers")
public class FilteringClientLoggingFilter extends BaseFilteringLogger implements ClientRequestFilter, ClientResponseFilter {

	public FilteringClientLoggingFilter(JerseyFiltering jerseyFiltering, LoggingFeature.Verbosity verbosity, int maxEntitySize) {
		super(jerseyFiltering,verbosity, maxEntitySize);
	}

	@Override
	public void filter(final ClientRequestContext context) throws IOException {
		String uriPath = context.getUri().getPath();
		if (!logger.isTraceEnabled() || jerseyFiltering.excludeForUri(uriPath)) {
			return;
		}
		long id = _id.incrementAndGet();
		context.setProperty(LOGGING_ID_PROPERTY, id);

		StringBuilder b = new StringBuilder();

		URI uri = context.getUri();
		ConnectContext.set(Constants.REST_CONTEXT, "sending - " + context.getMethod() + " " + uri.toASCIIString());

		printRequestLine(b, "Sending client request", id, context.getMethod(), uri);
		printPrefixedHeaders(b, id, REQUEST_PREFIX, context.getStringHeaders());


		if (context.hasEntity() && printEntity(verbosity, context.getMediaType()) && !jerseyFiltering.excludePayloadForUri(uriPath)) {
			OutputStream stream = new LoggingStream(b, context.getEntityStream());
			context.setEntityStream(stream);
			context.setProperty(ENTITY_LOGGER_PROPERTY, stream);
			// not calling log(b) here - it will be called by the interceptor
		} else {
			log(b);
		}
	}

	@Override
	public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext) throws IOException {
		String uriPath = requestContext.getUri().getPath();
		if (!logger.isTraceEnabled() || jerseyFiltering.excludeForUri(uriPath)) {
			return;
		}
		Object requestId = requestContext.getProperty(LOGGING_ID_PROPERTY);
		long id = requestId != null ? (Long) requestId : _id.incrementAndGet();

		StringBuilder b = new StringBuilder();
		printResponseLine(b, "Client response received", id, responseContext.getStatus());
		printPrefixedHeaders(b, id, RESPONSE_PREFIX, responseContext.getHeaders());

		if (responseContext.hasEntity() && printEntity(verbosity, responseContext.getMediaType())) {
			responseContext.setEntityStream(logInboundEntity(b, responseContext.getEntityStream(), MessageUtils.getCharset(responseContext.getMediaType())));
		}

		log(b);
	}
}
