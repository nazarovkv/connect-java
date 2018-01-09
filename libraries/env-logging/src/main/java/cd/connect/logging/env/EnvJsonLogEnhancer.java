package cd.connect.logging.env;

import cd.connect.logging.JsonLogEnhancer;
import com.bluetrainsoftware.common.config.ConfigKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Richard Vowles on 9/01/18.
 */
public class EnvJsonLogEnhancer implements JsonLogEnhancer {
	// ENV_VAR=localName
	@ConfigKey("connect.logging.environment")
	Map<String, String> environmentMap;

	private Map<String, String> converted = new ConcurrentHashMap<>();

	public void init() {
		environmentMap.forEach((env, logField) -> {
			String e = System.getenv(env);
			if (e != null) {
				e = e.trim();
				if (e.length() > 0) {
					converted.put(logField, e);
				}
			}
		});
	}

	@Override
	public int getMapPriority() {
		return 20;
	}

	@Override
	public void map(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects) {
		if (converted.size() > 0) {

		}
	}

	@Override
	public void failed(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects, Throwable e) {

	}
}
