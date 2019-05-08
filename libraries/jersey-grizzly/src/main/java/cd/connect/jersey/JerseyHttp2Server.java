package cd.connect.jersey;

import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http2.Http2AddOn;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class JerseyHttp2Server {
	/**
	 * Register a Grizzly HTTP/2 server and start it while also ensuring it shuts down incoming connections with
	 * a 10 second grace period
	 *
	 * @param config - Jersey resource config
	 * @param BASE_URI - url and port to mount on
	 * @throws IOException - if anything goes wrong
	 */
	public static void start(ResourceConfig config, URI BASE_URI) throws IOException {
		final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, false);
		final NetworkListener listener = server.getListeners().iterator().next();
		listener.registerAddOn(new Http2AddOn());

		ApplicationLifecycleManager.registerListener(trans -> {
			if (trans.next == LifecycleStatus.TERMINATING) {
				server.shutdown(10, TimeUnit.SECONDS);
			}
		});

		server.start();
	}
}
