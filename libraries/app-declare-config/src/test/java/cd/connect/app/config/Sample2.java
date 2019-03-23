package cd.connect.app.config;

import java.util.List;
import java.util.Map;

public class Sample2 {
  @ConfigKey("sample2.value")
  private List<String> value;
  @ConfigKey("sample2.value2")
  private Map<String,String> value2;
  @ConfigKey("sample2.dontset")
  private String alreadyDefault = "default";

  public Sample2() {
    DeclaredConfigResolver.resolve(this);
  }

  public List<String> getValue() {
    return value;
  }

  public Map<String, String> getValue2() {
    return value2;
  }

  public String getAlreadyDefault() {
    return alreadyDefault;
  }
}
