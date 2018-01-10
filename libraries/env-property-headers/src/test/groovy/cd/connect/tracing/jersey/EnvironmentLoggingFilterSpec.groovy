package cd.connect.tracing.jersey

import org.slf4j.MDC
import org.springframework.util.MultiValueMap
import spock.lang.Specification

import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap

/**
 * Created by Richard Vowles on 11/01/18.
 */
class EnvironmentLoggingFilterSpec extends Specification {
	def "basic test"() {
		given: "i have some sample environment variables"
		  def envs = ["MY_KUBE_NODE":"local", "MY_KUBE_REGION":"mars", 'MY_IGNORE': 'should-ignore-this']
		and: "i have a simple logger"
		  EnvironmentLoggingFilter filter = new EnvironmentLoggingFilter() {
			  @Override
			  protected String getEnv(String envName) {
				  return envs[envName]
			  }
		  }
		  filter.environmentMap = ['MY_KUBE_NODE':'X-Origin-Kube-Node','MY_KUBE_REGION':'X-Origin-Kube-Region', 'MY_IGNORE': 'X-Dont-Use-Local-Env', 'NO_SUCH_ENV':'X-I-Dont-Exist']
		  filter.init()
		and: "i have a container request with a single header"
			MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
			headers.add('X-Dont-Use-Local-Env', 'already-value')
			ContainerRequestContext incomingRequest = [getHeaderString: { h -> return headers.getFirst(h); }] as ContainerRequestContext
		and: "I have a client request going out with headers to fill up"
		  MultivaluedMap<String, String> requestheaders = new MultivaluedHashMap<>();
		  ClientRequestContext outgoingRequest = [getHeaders: { return requestheaders; }] as ClientRequestContext
		when: "i make an incoming request the logging context is filled in"
		  filter.filter(incomingRequest)
		and: "i make an outgoing request"
		  filter.filter(outgoingRequest)
		then: 'i should have an MDC of three items'
		  MDC.getCopyOfContextMap().size() == 3
		  MDC.get('origin.kube.node') == 'local'
		  MDC.get('origin.kube.region') == 'mars'
		  MDC.get('dont.use.local.env') == 'already-value'
		and: 'I the outgoing request should have those headers'
		  requestheaders.size() == 3
		  requestheaders.getFirst('X-Origin-Kube-Node') == 'local'
		  requestheaders.getFirst('X-Origin-Kube-Region') == 'mars'
			requestheaders.getFirst('X-Dont-Use-Local-Env') == 'already-value'
	}
}
