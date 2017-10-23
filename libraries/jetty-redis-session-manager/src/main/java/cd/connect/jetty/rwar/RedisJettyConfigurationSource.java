package cd.connect.jetty.rwar;

import cd.connect.war.WarConfigurationSource;
import org.eclipse.jetty.webapp.Configuration;

/**
 * The purpose of this is just to add the extra class to the WebAppRunner to
 * let it be involved in the creation of the WebAppContext.
 *
 * @author: Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class RedisJettyConfigurationSource implements WarConfigurationSource {
  @Override
  public Class<? extends Configuration> getConfiguration() {
    // ensure redis exists and is configured, and if it is, tell the WebAppRunner about the extra class it
    // will need to run
    if (System.getProperty("redis.port") != null && System.getProperty("redis.host") != null) {
      return RedisSessionDataStoreConfiguration.class;
    }

    return null;
  }
}
