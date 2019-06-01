package cd.connect.jersey;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http2.Http2AddOn;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class JerseyHttp2Server {
  private static final Logger log = LoggerFactory.getLogger(JerseyHttp2Server.class);
  @ConfigKey("server.port")
  Integer port = 8903;
  @ConfigKey("server.gracePeriodInSeconds")
  Integer gracePeriod = 10;

  public JerseyHttp2Server() {
    DeclaredConfigResolver.resolve(this);
  }

  public void start(ResourceConfig config) throws IOException {
    start(config, port, gracePeriod);
  }

  public void start(ResourceConfig config, int port) throws IOException {
    start(config, port, gracePeriod);
  }

  public void start(ResourceConfig config, int port, int gracePeriod) throws IOException {
    URI BASE_URI = URI.create(String.format("http://0.0.0.0:%d/", port));

    start(config, BASE_URI);
  }

	/**
	 * Register a Grizzly HTTP/2 server and start it while also ensuring it shuts down incoming connections with
	 * a 10 second grace period
	 *
	 * @param config - Jersey resource config
	 * @param BASE_URI - url and port to mount on
	 * @throws IOException - if anything goes wrong
	 */
	public static void start(ResourceConfig config, URI BASE_URI, int gracePeriod) throws IOException {
		final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, false);

		// add http2 to the mix
		final NetworkListener listener = server.getListeners().iterator().next();
		listener.registerAddOn(new Http2AddOn());

		ApplicationLifecycleManager.registerListener(trans -> {
			if (trans.next == LifecycleStatus.TERMINATING) {
        try {
          server.shutdown(gracePeriod, TimeUnit.SECONDS).get();
        } catch (InterruptedException|ExecutionException e) {
          log.error("Failed to shutdown server in {} seconds", gracePeriod);
        }
      }
		});

		server.start();

		log.info("server started on {} with http/2 enabled", BASE_URI.toString());
	}

  public static void start(ResourceConfig config, URI BASE_URI) throws IOException {
	  start(config, BASE_URI, 10);
  }
}
