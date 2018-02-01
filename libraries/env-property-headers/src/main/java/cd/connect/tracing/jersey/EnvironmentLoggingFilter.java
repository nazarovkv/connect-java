package cd.connect.tracing.jersey;

import com.bluetrainsoftware.common.config.ConfigKey;
import net.stickycode.stereotype.configured.PostConfigured;

import java.util.HashMap;
import java.util.Map;

/**
 * These are created as needed by Jersey.
 *
 * Created by Richard Vowles on 11/01/18.
 */
public class EnvironmentLoggingFilter extends BaseLoggingFilter {
	private Map<String, String> localEnvironmentMap = new HashMap<>();

	public EnvironmentLoggingFilter() {
		makeMapFromConfig("connect.logging.headers.from-environment").forEach((envName, headerName) -> {
			String val = getEnv(envName);

			if (val != null) {
				localEnvironmentMap.put(headerName, val);
			}

			allHeaderNames.add(headerName);

			headerLogNameMap.put(headerName, headerNameToLogName(headerName));
		});
	}

	// not writable in tests, so allow it to be overridden.
	protected String getEnv(String envName) {
		return System.getenv(envName);
	}

	protected String getLocalValueForMissingHeader(String headerName) {
		return localEnvironmentMap.get(headerName);
	}
}
