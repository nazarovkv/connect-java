package cd.connect.tracing.jersey;

import org.slf4j.MDC;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Richard Vowles on 11/01/18.
 */
abstract public class BaseLoggingFilter implements ContainerRequestFilter, ClientRequestFilter {
	protected List<String> allHeaderNames = new ArrayList<>();
	protected Map<String, String> headerLogNameMap = new HashMap<>();

	// strip x-, replace - with .
	protected String headerNameToLogName(String name) {
		name = name.toLowerCase();

		if (name.startsWith("x-")) {
			name = name.substring(2);
		}

		return name.replace("-", ".");
	}


	abstract protected String getLocalValueForMissingHeader(String headerName);

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		allHeaderNames.forEach(headerName -> {
			String val = requestContext.getHeaderString(headerName);

			if (val == null) {
				val = getLocalValueForMissingHeader(headerName);
			}

			if (val != null) {
				MDC.put(headerLogNameMap.get(headerName), val);
			}
		});
	}

	/**
	 * We are making a request out.
	 *
	 * @param requestContext - the context to append headers to.
	 * @throws IOException - a failure if any
	 */
	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		MultivaluedMap<String, Object> headers = requestContext.getHeaders();

		allHeaderNames.forEach(headerName -> {
			String val = MDC.get(headerLogNameMap.get(headerName));
			if (val == null) {
				val = getLocalValueForMissingHeader(headerName); // in case we originated this call
			}
			if (val != null) {
				headers.add(headerName, val);
			}
		});
	}
}
