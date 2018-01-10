package cd.connect.logging.env;

import cd.connect.logging.JsonLogEnhancer;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Richard Vowles on 9/01/18.
 */
public class EnvJsonLogEnhancer implements JsonLogEnhancer {
	private Map<String, String> converted = new ConcurrentHashMap<>();
	final protected static String CONFIG_KEY = "connect.logging.environment";

	public EnvJsonLogEnhancer() {
		String envs = System.getProperty(CONFIG_KEY);
		if (envs != null) {
			StringTokenizer st = new StringTokenizer(envs, ",");
			while (st.hasMoreTokens()) {
				String[] val = st.nextToken().split("=");
				if (val.length == 2) { // two parts
					String e = getEnv(val[0]);
					if (e != null) {
						e = e.trim();
						if (e.length() > 0) {
							converted.put(val[1], e);
						}
					}
				}
			}
		}
	}

	protected String getEnv(String env) {
		return System.getenv(env);
	}

	@Override
	public int getMapPriority() {
		return 20;
	}

	@Override
	public void map(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects) {
		converted.forEach(log::put);
	}

	@Override
	public void failed(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects, Throwable e) {
	}
}
