package cd.connect.jetty.redis;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonSerializer extends SerializerSkeleton {
  private static final Logger log = LoggerFactory.getLogger(JsonSerializer.class);
  public ObjectMapper mapper;

  @Override
  public void start() {
    mapper = new ObjectMapper();
    mapper.disable(SerializationFeature.WRAP_ROOT_VALUE);

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, false);
    mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    mapper.setVisibility(new VisibilityChecker.Std(ANY, ANY, ANY, ANY, ANY));

    super.start();
  }

  @Override
  public void stop() {
    super.stop();
    mapper = null;
  }

  @Override
  public String serialize(Object o) throws SerializerException {
    try {
      return mapper.writeValueAsString(o);
    } catch (IOException e) {
      throw new SerializerException(e);
    }
  }

  @Override
  public String serializeSessionAttributes(Map<String, Object> map) throws SerializerException {
    ArrayNode array = mapper.createArrayNode();
    for (Entry<String, Object> entry : map.entrySet()) {
      array.add(convertToNode(entry.getKey(), entry.getValue()));
    }
    ObjectNode n = mapper.createObjectNode();
    n.set("attibutes", array);
    try {
      return mapper.writeValueAsString(n);
    } catch (IOException e) {
      throw new SerializerException(e);
    }
  }



  private JsonNode convertToNode(String key, Object value) {
    JsonNode valueToTree = mapper.valueToTree(value);

    ObjectNode o;

    if (valueToTree.isObject()) {
      o = (ObjectNode) valueToTree;
      o.put("attributeName", key);
      o.put("attributeType", value.getClass().getName());
    } else {
      o = mapper.createObjectNode();
      o.put("value", value.toString());
      o.put("attributeName", key);
      o.put("attributeType", value.getClass().getName());
    }

    return o;
  }

  @Override
  public <T> T deserialize(String o, Class<T> targetType) throws SerializerException {
    try {
      return mapper.readValue(o, targetType);
    } catch (IOException e) {
      throw new SerializerException(e);
    }
  }

  private static ConcurrentHashMap<String, Constructor> constructors = new ConcurrentHashMap<>();

  private Object getInstance(String attributeType, String value) {
    Constructor c = constructors.get(attributeType);
    if (c == null) {
      try {
        c = Class.forName(attributeType).getConstructor(String.class);
        if (c != null) {
          constructors.put(attributeType, c);
        }
      } catch (NoSuchMethodException|ClassNotFoundException e) {
        log.error("Cannot create {} from {}", value, attributeType);
      }
    }

    if (c != null) {
      try {
        return c.newInstance(value);
      } catch (InstantiationException|IllegalAccessException|InvocationTargetException e) {
        log.error("Cannot create {} from {}", value, attributeType);
      }
    }

    return null;
  }

  @Override
  public Map<String, Object> deserializeSessionAttributes(String o) throws SerializerException {
    Map<String, Object> ret = new HashMap<String, Object>();
    try {
      JsonNode arrNode = mapper.readTree(o).get("attibutes");
      if (arrNode.isArray()) {
        for (final JsonNode objNode : arrNode) {
          JsonNode name = objNode.get("attributeName");
          JsonNode type = objNode.get("attributeType");
          if (name != null && type != null) {
            String attributeName = name.asText();
            String attributeType = type.asText();
            if (attributeType.startsWith("java.lang.")) {
              ret.put(attributeName, getInstance(attributeType, objNode.get("value").asText()));
            } else {
              ret.put(attributeName, mapper.treeToValue(objNode, Class.forName(attributeType)));
            }
          }
        }
      }
    } catch (JsonProcessingException e) {
      throw new SerializerException(e);
    } catch (IOException e) {
      throw new SerializerException(e);
    } catch (ClassNotFoundException e) {
      throw new SerializerException(e);
    }
    return ret;
  }
}
