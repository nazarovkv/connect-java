package cd.connect.jetty.redis;

import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class PooledJedisExecutor implements JedisExecutor {
  private final JedisPool jedisPool;
  private final String url;

  public PooledJedisExecutor(JedisPool jedisPool, String host, int port) {
    this.jedisPool = jedisPool;
    this.url = host + ":" + port;
  }

  @Override
  public <V> V execute(String operationName, JedisCallback<V> cb) {
    Tracer tracer = GlobalTracer.get();
    ActiveSpan activeSpan = tracer.activeSpan();

    Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName)
      .withTag(Tags.DB_TYPE.getKey(), "redis")
      .withTag(Tags.DB_INSTANCE.getKey(), url);

    if (activeSpan != null) {
      spanBuilder.asChildOf(activeSpan);
    }

    ActiveSpan span = spanBuilder.startActive();

    try (Jedis jedis = jedisPool.getResource()) {
      return cb.execute(jedis);
    } finally {
      if (span != null) {
        span.close();
      }
    }
  }
}
