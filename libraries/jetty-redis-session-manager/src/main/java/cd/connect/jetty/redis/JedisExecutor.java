package cd.connect.jetty.redis;

interface JedisExecutor {
    <V> V execute(String operationName, JedisCallback<V> cb);
}
