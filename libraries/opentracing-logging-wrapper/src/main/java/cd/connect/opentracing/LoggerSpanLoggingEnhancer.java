package cd.connect.opentracing;

import cd.connect.logging.JsonLogEnhancer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cd.connect.opentracing.OpenTracingLogger.WELL_KNOWN_REQUEST_ID;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class LoggerSpanLoggingEnhancer implements JsonLogEnhancer {
  @Override
  public int getMapPriority() {
    return 30;
  }



  private void pushOpenTracing(Map<String, Object> log, Map<String, Object> yours) {
    if (yours.size() > 0) {
      Map<String, Object> opentracing = (Map<String, Object>)log.computeIfAbsent("opentracing", e -> new HashMap<>());
      opentracing.putAll(yours);
      log.put("opentracing", opentracing);
    }
  }

  @Override
  public void map(Map<String, String> context,
                  Map<String, Object> log,
                  List<String> alreadyEncodedJsonObjects) {
    String baggageJson = context.remove(LoggerSpan.OPENTRACING_BAGGAGE);
    String logsJson = context.remove(LoggerSpan.OPENTRACING_LOG_MESSAGES);
    String tagsJson = context.remove(LoggerSpan.OPENTRACING_TAGS);
    context.remove(LoggerSpan.OPENTRACING_ID);
    
    pushOpenTracing(log, ObjectMapperProvider.unwrapMap(baggageJson));

    // maybe should wipe these after logging them once
    pushOpenTracing(log, ObjectMapperProvider.unwrapMap(logsJson));
    pushOpenTracing(log, ObjectMapperProvider.unwrapMap(tagsJson));
  }

  @Override
  public void failed(Map<String, String> context,
                     Map<String, Object> log,
                     List<String> alreadyEncodedJsonObjects, Throwable e) {

  }
}
