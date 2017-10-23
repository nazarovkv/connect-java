package cd.connect.war;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class SampleLifecycleListener implements WarLifecycleListener {

	@Override
	public void lifeCycleStarting(Server server, WebAppContext context) {

	}

	@Override
	public void lifeCycleStarted(Server server, WebAppContext context) {

	}

	@Override
	public void lifeCycleFailure(Server server, Throwable cause, WebAppContext context) {

	}

	@Override
	public void lifeCycleStopping(Server server, WebAppContext context) {

	}

	@Override
	public void lifeCycleStopped(Server server, WebAppContext context) {

	}
}
