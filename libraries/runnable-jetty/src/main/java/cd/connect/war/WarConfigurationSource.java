package cd.connect.war;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface WarConfigurationSource {
	Class<? extends org.eclipse.jetty.webapp.Configuration> getConfiguration();
}
