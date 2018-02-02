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
	protected Map<String, String> converted = new ConcurrentHashMap<>();
	final protected static String CONFIG_KEY = "connect.logging.environment";
	protected static EnvJsonLogEnhancer self;

  /**
   * The difficulty here is that the logging gets initialized _very_ early. So you need to choose something
   * that works with your framework that will initialize it after the properties have been set. This may
   * be on the command line, in which case up front is fine. Otherwise later will have to do.
   *
   * Command line will work out of the box, i.e. -Dconnect.logging.environment, otherwise call it when you can
   */
	public static void initialize() {
	  if (self != null) {
      String envs = System.getProperty(CONFIG_KEY);
      if (envs != null && self.converted.size() == 0) { // don't init twice
        StringTokenizer st = new StringTokenizer(envs, ",");
        while (st.hasMoreTokens()) {
          String[] val = st.nextToken().split("[:=]");
          if (val.length == 2) { // two parts
            String e = self.getEnv(val[0]);
            if (e != null) {
              e = e.trim();
              if (e.length() > 0) {
                self.converted.put(val[1], e);
              }
            }
          }
        }
      }
    }
  }

	public EnvJsonLogEnhancer() {
    self = this;
    initialize();
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
