package cd.connect.tracing.jersey

import cd.connect.tracing.HeaderLoggingConfiguration
import cd.connect.tracing.extractors.NoopTracingExtractor
import cd.connect.tracing.extractors.ZipkinIstioTracingExtractor
import com.uber.jaeger.SpanContext
import io.opentracing.ActiveSpan
import io.opentracing.Tracer
import org.slf4j.MDC
import spock.lang.Specification

import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.core.MultivaluedHashMap

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class ClientLoggingOpenTracingAdapterFilterSpec extends Specification {
	def "basic operation works"() {
		given: "i have an header config"
			HeaderLoggingConfiguration hlc = new HeaderLoggingConfiguration(null)
			hlc.configuredPropagateHeaders = ['baggage', 'X-Header=sausage']
			hlc.appName = 'mary'
			hlc.completeConfiguration()
		and: "a fake tracer"
		  // tracer baggage contains these, so they shouldn't be added by the client
		  Map<String, String> baggages = ['baggage': 'luggage', 'flavour': 'soy sauce']

			Tracer tracer = [activeSpan: { -> return [
				context: { -> return new SpanContext(1L, 2L, 3L, (byte)0) },
				getBaggageItem: { key ->
					return baggages[key]
				}
			] as ActiveSpan }] as Tracer
		and: "i have a client request"
		  MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<String, String>()
		  ClientRequestContext ctx = [getHeaders: {->
				return headers
			}] as ClientRequestContext
		and: "i have the client request filter"
		  ClientLoggingOpenTracingAdapterFilter clf = new ClientLoggingOpenTracingAdapterFilter(hlc, tracer, new NoopTracingExtractor(hlc))
		and: "I set the MDC"
		  MDC.put("sausage", "cumberlands")
		  MDC.put("flavour", "tempura")
		when: "i ask for the filter"
		  clf.filter(ctx)
		then:
		  headers.size() == 1
		  headers.getFirst("X-Header") == "cumberlands"
	}
}
