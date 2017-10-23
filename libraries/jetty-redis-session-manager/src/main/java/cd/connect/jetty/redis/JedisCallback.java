package cd.connect.jetty.redis;

import redis.clients.jedis.Jedis;

interface JedisCallback<V> {
    V execute(Jedis jedis);
}
