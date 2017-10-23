package cd.connect.logging;

import cd.connect.context.ConnectContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * This is the basic support across logging vendors for mapping json
 * objects to the right constructs and adding in extra basic info.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class DefaultJsonLogEnhancer implements JsonLogEnhancer {
  static int prefixLen = ConnectContext.JSON_PREFIX.length();
  private final String hostName;

  public DefaultJsonLogEnhancer() {
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int getMapPriority() {
    return 100;
  }

  @Override
  public void map(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects) {
    log.put("host", hostName);

    context.entrySet().forEach(entry -> {
      if (entry.getKey().startsWith(ConnectContext.JSON_PREFIX)) {
        alreadyEncodedJsonObjects.add(
          "\"" + entry.getKey().substring(prefixLen) + "\":" + entry.getValue());
      } else {
        log.put(entry.getKey(), entry.getValue());
      }
    });
  }

  @Override
  public void failed(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects, Throwable e) {
    // meh
  }
}
