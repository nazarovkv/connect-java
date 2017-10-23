package cd.connect.context;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface NamedConnectContext {
  // all http based incoming traffic must have a request id
  public static final String requestId = "request-id";
  // cucumber testing is a core part of Connect, hence the scenario id
  public static final String scenarioId = "scenario-id";

  String get();
  void set(String id);
  void remove();
}
