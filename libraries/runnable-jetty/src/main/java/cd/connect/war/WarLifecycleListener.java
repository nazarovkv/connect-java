package cd.connect.war;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Exposing the internal interface.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
 public interface WarLifecycleListener {
	 void lifeCycleStarting(Server server, WebAppContext context);
	 void lifeCycleStarted(Server server, WebAppContext context);
	 void lifeCycleFailure(Server server,Throwable cause, WebAppContext context);
	 void lifeCycleStopping(Server server, WebAppContext context);
	 void lifeCycleStopped(Server server, WebAppContext context);
}
