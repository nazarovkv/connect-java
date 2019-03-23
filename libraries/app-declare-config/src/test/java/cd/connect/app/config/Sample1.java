package cd.connect.app.config;

public class Sample1 {
  @ConfigKey("sample1.value")
  private String value;
  @ConfigKey("sample1.value2")
  private Integer value2;

  public Sample1() {
    DeclaredConfigResolver.resolve(this);
  }

  public String getValue() {
    return value;
  }

  public Integer getValue2() {
    return value2;
  }
}
