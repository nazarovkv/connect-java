package cd.connect.tracing.jersey;

import com.bluetrainsoftware.common.config.ConfigKey;
import net.stickycode.stereotype.configured.PostConfigured;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Richard Vowles on 11/01/18.
 */
public class EnvironmentLoggingFilter extends BaseLoggingFilter {

	@ConfigKey("connect.logging.headers.from-environment")
	Map<String, String> environmentMap;

	private Map<String, String> localEnvironmentMap = new HashMap<>();

	@PostConfigured
	public void init() {
		environmentMap.forEach((envName, headerName) -> {
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
