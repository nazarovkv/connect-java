package cd.connect.tracing;

import java.util.List;

/**
 * Allows implementations to register their headers they want propagated.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface HeaderLoggingConfigurationSource {
  List<String> getHeaderLoggingConfig();
}
