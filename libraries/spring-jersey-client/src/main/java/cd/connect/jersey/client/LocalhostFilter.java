package cd.connect.jersey.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class LocalhostFilter implements ClientRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(LocalhostFilter.class);
  private final boolean rewriting;

  public LocalhostFilter() {
    rewriting = System.getProperty("forceLocalhost") != null;
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    if (rewriting) {
      log.info("previous uri {}", requestContext.getUri().toString());

	    UriBuilder uriBuilder = UriBuilder.fromUri(requestContext.getUri());
	    uriBuilder.host(System.getProperty(requestContext.getUri().getHost(), "127.0.0.1"));
	    requestContext.setUri(uriBuilder.build());

      log.info("current uri {}", requestContext.getUri().toString());
    }
  }
}

