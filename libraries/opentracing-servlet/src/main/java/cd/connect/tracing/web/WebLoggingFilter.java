package cd.connect.tracing.web;

import cd.connect.tracing.HeaderLoggingConfiguration;
import cd.connect.tracing.extractors.TracingExtractor;
import cd.connect.tracing.jersey.LoggingContextResponse;
import com.bluetrainsoftware.common.config.ConfigKey;
import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import net.stickycode.stereotype.configured.PostConfigured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;


/**
 *
 * You need to ensure this filter triggers *after* your opentracing one. Otherwise the appropriate SpanContext will not
 * exist.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class WebLoggingFilter implements Filter {
	private static final Logger log = LoggerFactory.getLogger(WebLoggingFilter.class);
	@ConfigKey("connect.logging.skipPattern")
	protected String skip = "";
	private final HeaderLoggingConfiguration headerLoggingConfiguration;
	private final Tracer tracer;
	private final TracingExtractor tracingExtractor;

	private Pattern skipPattern;

	@Inject
	public WebLoggingFilter(HeaderLoggingConfiguration headerLoggingConfiguration, Tracer tracer, TracingExtractor tracingExtractor) {
		this.headerLoggingConfiguration = headerLoggingConfiguration;
		this.tracer = tracer;
		this.tracingExtractor = tracingExtractor;
	}

	@PostConfigured
	public void init() {
		if (skip.length() > 0) {
			skipPattern = Pattern.compile(skip);
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	private boolean isLogged(String url) {
		// skip URLs matching skip pattern
		// e.g. pattern is defined as '/health|/status' then URL 'http://localhost:5000/context/health' won't be traced
		return skipPattern == null || !skipPattern.matcher(url).matches();

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest req = HttpServletRequest.class.cast(request);
		final HttpServletResponse res = HttpServletResponse.class.cast(response);

		String fullPath = req.getRequestURI().substring(req.getContextPath().length());

		long start = System.currentTimeMillis();

		try {
			if (!isLogged(fullPath)) {
				chain.doFilter(request, response);
			} else {
				processHeaders(req);

				chain.doFilter(request, response);
				// this exists for Rob's visualization
				LoggingContextResponse.toJsonLog(res.getStatus(), (int)(System.currentTimeMillis() - start));

				if (res.getStatus() < 500) {
					log.debug("request-complete: {}", fullPath);
				} else {
					log.error("request-complete: {}", fullPath);
				}
			}
		} finally {
			MDC.clear();
		}
	}

	protected void processHeaders(final HttpServletRequest req) {
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
				baggage = req.getHeader(acceptHeader);

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

		tracingExtractor.embedActiveSpanContext(activeSpan == null ? null : activeSpan.context(), req::getHeader);
	}

	@Override
	public void destroy() {
	}
}
