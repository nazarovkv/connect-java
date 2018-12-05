package cd.connect.opentracing;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class OpenTracingLogger {
  public static String WELL_KNOWN_REQUEST_ID = "requestId";
  public static String WELL_KNOWN_ORIGIN_APP = "originApp";
  public static String WELL_KNOWN_SCENARIO_ID = "scenarioId";

  // replace this with something else if you want to use a different string provider
  public static Supplier<String> randomRequestIdProvider = () -> {
    return UUID.randomUUID().toString();
  };
}
