package cd.connect.jetty.redis;

public abstract class SerializerSkeleton implements Serializer {
    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String serialize(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
