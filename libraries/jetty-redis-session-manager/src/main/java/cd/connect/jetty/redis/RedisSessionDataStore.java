package cd.connect.jetty.redis;

import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Transaction;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class RedisSessionDataStore extends AbstractSessionDataStore {
  private static final Logger log = LoggerFactory.getLogger(RedisSessionDataStore.class);
  private final PooledJedisExecutor jedisPool;
  private final Serializer serializer;

  public RedisSessionDataStore(PooledJedisExecutor jedisPool, Serializer serializer) {
    this.jedisPool = jedisPool;
    this.serializer = serializer;
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();

    serializer.start();
  }

  @Override
  protected void doStop() throws Exception {
    super.doStop();

    serializer.stop();
  }

  private Map<String, String>sessionToMap(SessionData data) {
    final Map<String, String> redisMap = new TreeMap<>();

    redisMap.put("id", data.getId());
    redisMap.put("cpath", data.getContextPath());
    redisMap.put("vhost", data.getVhost());
    redisMap.put("created", Long.toString(data.getCreated()));
    redisMap.put("lastAccessed", Long.toString(data.getLastAccessed()));
    redisMap.put("accessed", Long.toString(data.getAccessed()));
    redisMap.put("maxInactiveMs", Long.toString(data.getMaxInactiveMs()));
    redisMap.put("cookieSet", Long.toString(data.getCookieSet()));
    redisMap.put("attributes", serializer.serializeSessionAttributes(data.getAllAttributes()));

    return redisMap;
  }

  @Override
  public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
    final Map<String, String> toStore = sessionToMap(data);

    log.debug("[RedisSessionManager] storeSession - Storing session id={}", data.getVhost());

    jedisPool.execute("sessionStore", jedis -> {
      data.setLastSaved(System.currentTimeMillis());

      toStore.put("lastSaved", Long.toString(data.getLastSaved()));
      Transaction multi = jedis.multi();

      final String key = key(data.getId());
      multi.hmset(key, toStore);
      long ttl = data.getMaxInactiveMs() * 1000;
      if (ttl > 0) {
        multi.expire(key, (int)ttl);
      }

      return multi.exec();
    });
  }

  private String key(String id) {
    return "jetty-session-" + id;
  }

  @Override
  public Set<String> doGetExpired(Set<String> candidates) {
    return new HashSet<>();
  }

  @Override
  public boolean isPassivating() {
    return false;
  }

  @Override
  public boolean exists(String id) throws Exception {
    return jedisPool.execute("sessionExists", jedis -> {
      return jedis.exists(key(id));
    });
  }

  @Override
  public SessionData load(String id) throws Exception {
    return jedisPool.execute("sessionLoad", jedis -> {
      List<String> vals = jedis.hmget(key(id), "cpath", "vhost", "created", "accessed", "lastAccessed", "maxInactiveMs", "attributes", "cookieSet" );

      if (vals.size() == 0 || vals.get(0) == null) {
        return null;
      }

      SessionData data = new SessionData(id, vals.get(0), vals.get(1), Long.parseLong(vals.get(2)), Long.parseLong(vals.get(3)), Long.parseLong(vals.get(4)),
         Long.parseLong(vals.get(5)), serializer.deserializeSessionAttributes(vals.get(6)));

      data.setCookieSet(Long.parseLong(vals.get(7)));

      return data;
    });
  }

  @Override
  public boolean delete(String id) throws Exception {
    return jedisPool.execute("sessionDelete", jedis -> {
      return jedis.del(key(id));
    }) == 1;
  }
}
