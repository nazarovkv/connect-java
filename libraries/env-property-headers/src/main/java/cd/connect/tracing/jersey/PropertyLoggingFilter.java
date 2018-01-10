package cd.connect.tracing.jersey;

import com.bluetrainsoftware.common.config.ConfigKey;
import net.stickycode.stereotype.configured.PostConfigured;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Richard Vowles on 11/01/18.
 */
public class PropertyLoggingFilter extends BaseLoggingFilter {
	@ConfigKey("connect.logging.headers.from-properties")
	Map<String, String> propertyMap;

	Map<String, String> headerFirstPropertyMap = new HashMap<>();

	@PostConfigured
	public void init() {
		propertyMap.forEach((propertyName, headerName) -> {
			// we need this to go from a header -> a property name to extract the value
			headerFirstPropertyMap.put(headerName, propertyName);

			allHeaderNames.add(headerName);

			headerLogNameMap.put(headerName, headerNameToLogName(headerName));
		});
	}

	@Override
	protected String getLocalValueForMissingHeader(String headerName) {
		return System.getProperty(headerFirstPropertyMap.get(headerName));
	}
}
