package cd.connect.jetty.redis;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Warning - DOES NOT HANDLE PRIMITIVE ARRAYS!
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class JsonSerializerTests {

  private static final String MONKEY_OBJECT = "{\"attibutes\":[{\"bananas\":5,\"attributeName\":\"m\",\"attributeType\":\"com.globaldairytrade.common.session.serializer.Monkey\"}]}";
  private static final String MONKEY_STRING = "{\"attibutes\":[{\"value\":\"Monkey\",\"attributeName\":\"m\",\"attributeType\":\"java.lang.String\"}]}";
  private static final String MONKEY_INTEGER = "{\"attibutes\":[{\"value\":\"5\",\"attributeName\":\"m\",\"attributeType\":\"java.lang.Integer\"}]}";

  JsonSerializer js;
  Map<String, Object> map;

  @Before
  public void init() {
    js = new JsonSerializer();
    js.start();
    map  = new HashMap<>();
  }

  @Test
  public void objectSerializeAttribute() {
    Monkey m = new Monkey();
    m.bananas = 5;

    map.put("m", m);
    String val = js.serializeSessionAttributes(map);

    assertThat(val).isEqualTo(MONKEY_OBJECT);
  }

  @Test
  public void stringSerializeAttribute() {

    map.put("m", "Monkey");
    String val = js.serializeSessionAttributes(map);

    assertThat(val).isEqualTo(MONKEY_STRING);
  }

  @Test
  public void integerSerializeAttribute() {
    map.put("m", 5);
    String val = js.serializeSessionAttributes(map);

    assertThat(val).isEqualTo(MONKEY_INTEGER);
  }

  @Test
  public void deserializeMonkeyObject() {
    map = js.deserializeSessionAttributes(MONKEY_OBJECT);

    assertThat(map.get("m")).isInstanceOf(Monkey.class);
    assertThat(Monkey.class.cast(map.get("m")).bananas).isEqualTo(5);
  }

  @Test
  public void deserializeMonkeyString() {
    map = js.deserializeSessionAttributes(MONKEY_STRING);

    assertThat(map.get("m")).isInstanceOf(String.class);
    assertThat(String.class.cast(map.get("m"))).isEqualTo("Monkey");
  }

  @Test
  public void deserializeMonkeyInt() {
    map = js.deserializeSessionAttributes(MONKEY_INTEGER);

    assertThat(map.get("m")).isInstanceOf(Integer.class);
    assertThat(Integer.class.cast(map.get("m"))).isEqualTo(5);
  }
}
