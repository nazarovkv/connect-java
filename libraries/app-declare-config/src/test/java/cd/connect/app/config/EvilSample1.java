package cd.connect.app.config;

// this one doesn't have a default value, so if no system property it should barf.
public class EvilSample1 {
  @ConfigKey("no-default-value")
  private Integer noDefaultValue;

  public EvilSample1() {
    DeclaredConfigResolver.resolve(this);
  }
}
