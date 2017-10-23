package cd.connect.jetty.redis;

import java.util.Map;

public interface Serializer {

    void start();

    void stop();

    String serialize(Object o) throws SerializerException;

    String serializeSessionAttributes(Map<String, Object> map) throws SerializerException;

    <T> T deserialize(String o, Class<T> targetType) throws SerializerException;

    Map<String, Object> deserializeSessionAttributes(String o) throws SerializerException;
}
