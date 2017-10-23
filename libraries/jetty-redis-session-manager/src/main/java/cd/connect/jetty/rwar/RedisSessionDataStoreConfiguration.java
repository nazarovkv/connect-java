package cd.connect.jetty.rwar;

import cd.connect.jetty.redis.PooledJedisExecutor;
import cd.connect.jetty.redis.RedisSessionDataStore;
import cd.connect.jetty.redis.JsonSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

/**
 * This participates in the Jetty configuration process by providing the SessionIdManager that
 * exists here. It pulls the configuration from the system properties.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class RedisSessionDataStoreConfiguration extends AbstractConfiguration {
	private static final Logger log = LoggerFactory.getLogger(RedisSessionDataStoreConfiguration.class);

  protected void configureRedis(SessionHandler handler, int port, String host) {
    log.info("RedisSessionManager: configuring Redis Session Handler on `{}:{}`", host, port);

    // now do what the jetty.xml was doing
    PooledJedisExecutor pooledExecutor = new PooledJedisExecutor(new JedisPool(new GenericObjectPoolConfig(), host, port), host, port);

    handler.setSessionCache(new DefaultSessionCache(handler));
    handler.getSessionCache().setSessionDataStore(new RedisSessionDataStore(pooledExecutor, new JsonSerializer()));
  }

	@Override
	public void configure(WebAppContext context) throws Exception {
    String redisPort = System.getProperty("redis.port", null);
    String redisHost = System.getProperty("redis.host", null);

    if (redisPort != null && redisHost != null) {
      configureRedis(context.getSessionHandler(), Integer.parseInt(redisPort.trim()), redisHost.trim());
    } else {
      log.warn("RedisSessionManager: not redis config, ignoring");
    }
  }
}
