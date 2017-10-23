package cd.connect.jersey.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.List;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class JaxrsClientManager implements JaxrsClient {
  protected Client client;

  public JaxrsClientManager(List<JaxrsClientConfigurer> configurers) {
    ClientBuilder client = ClientBuilder.newBuilder();

    configurers.forEach(c -> c.configure(client));

    this.client = client.build();
  }

  public Client getClient() {
    return client;
  }
}
