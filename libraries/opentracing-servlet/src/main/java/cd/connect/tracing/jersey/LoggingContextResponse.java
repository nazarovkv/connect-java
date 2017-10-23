package cd.connect.tracing.jersey;

import cd.connect.context.ConnectContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;

/**
 * Allows us to have a structured response in the logs
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class LoggingContextResponse {
  public int status;
  public int processingTimeMs;

  private static final ObjectMapper mapper = new ObjectMapper();

  public static void toJsonLog(int status, int processingTimeMs) {
    LoggingContextResponse r = new LoggingContextResponse();
    r.status = status;
    r.processingTimeMs = processingTimeMs;

    try {
      MDC.put(ConnectContext.JSON_PREFIX + "response-status", mapper.writeValueAsString(r));
    } catch (JsonProcessingException e) {
    }
  }
}
