package cd.connect.opentracing;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class OpenTracingLogger {
  public static String WELL_KNOWN_REQUEST_ID = "requestid";
  public static String WELL_KNOWN_ORIGIN_APP = "originApp";
  public static String WELL_KNOWN_SCENARIO_ID = "scenarioid";

  public static List<String> WELL_KNOWN_KEYS = Arrays.asList(WELL_KNOWN_REQUEST_ID, WELL_KNOWN_ORIGIN_APP, WELL_KNOWN_SCENARIO_ID);

  // replace this with something else if you want to use a different string provider
  public static Supplier<String> randomRequestIdProvider = () -> {
    return UUID.randomUUID().toString();
  };
}
