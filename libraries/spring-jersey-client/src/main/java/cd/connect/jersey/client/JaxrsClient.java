package cd.connect.jersey.client;

import javax.ws.rs.client.Client;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface JaxrsClient {
  Client getClient();
}
