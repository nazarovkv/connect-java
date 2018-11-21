package cd.connect.tracing;

import com.bluetrainsoftware.common.config.ConfigKey;
import net.stickycode.stereotype.configured.PostConfigured;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Centralizes all of the header propagation malarky.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Component
public class HeaderLoggingConfiguration {
  @ConfigKey("app.name")
  protected String appName;
  @ConfigKey("connect.logging.headers")
  protected List<String> configuredPropagateHeaders = new ArrayList<>();

  private List<HeaderLoggingConfigurationSource> headerSources;

  /**
   * this allows us to have mapping headers - i.e. we see X-Client-RequestID and turn it into request-id in the MDC,
   * but also accept request-id directly. The keys are the headers, the values are the values we put into the MDC.
   * If there is no "=", they are the same.
   *
   * It is used by the *server*.
   */
  private Map<String, String> headerToLoggingMapping;

	/**
	 * This is just a short-form of all the headers we are ok with so it is quick to cycle through it, we use it
	 * again and again on every request.
	 */
	private Set<String> actualHeaders;

  @Inject
  public HeaderLoggingConfiguration(List<HeaderLoggingConfigurationSource> headerSource) {
    this.headerSources = headerSource;
  }

  private void split(String header, Map<String, String> headerFirst) {
    int eq = header.indexOf('=');
    String incomingHeader;
    String incomingLog;

    if (eq  > 0 ) {
    	incomingHeader = header.substring(0, eq).trim();
      incomingLog = header.substring(eq+1).trim();
    } else {
    	incomingHeader =  header.trim();
    	incomingLog = incomingHeader;
    }

    headerFirst.put(incomingHeader, incomingLog);
  }

  // gets called every time configuration changes
  @PostConfigured
  public void completeConfiguration() {
    Map<String, String> headerFirst = new HashMap<>();

    configuredPropagateHeaders.forEach(h -> {
      split(h, headerFirst);
    });

    if (headerSources != null) {
      headerSources.forEach(s -> {
        s.getHeaderLoggingConfig().forEach(h -> {
          split(h, headerFirst);
        });
      });
    }

    headerToLoggingMapping = Collections.unmodifiableMap(headerFirst);

    this.actualHeaders = Collections.unmodifiableSet(headerFirst.keySet());
  }

  /**
   * header to logging field mappings
   *
   * @return unmodifiable map containing the headers and their mapping to their new name
   */
  public Map<String, String> getHeaderToLoggingMapping() {
    return headerToLoggingMapping;
  }

  public Set<String> getAcceptHeaders() {
  	if (actualHeaders == null) {
  		completeConfiguration();
	  }
	  
  	return actualHeaders;
  }

  public String getAppName() {
    return appName;
  }
}
