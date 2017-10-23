package cd.connect.war

import org.eclipse.jetty.util.component.LifeCycle
import spock.lang.Specification

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class LifecycleSpec extends Specification {
	def "sub-interfaces-can-be-found"() {
		when: "i load the listener"
		  def listeners = ServiceLoader.load(WarLifecycleListener.class)
		then: "i find one"
		  listeners.size() == 1
		  listeners[0].getClass() == SampleLifecycleListener.class
	}
}
