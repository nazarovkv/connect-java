package cd.connect.tracing.jersey

import cd.connect.tracing.HeaderLoggingConfiguration
import cd.connect.tracing.extractors.TracingExtractor
import cd.connect.tracing.extractors.JaegerTracingExtractor
import cd.connect.tracing.extractors.NoopTracingExtractor
import com.uber.jaeger.SpanContext
import io.opentracing.ActiveSpan
import io.opentracing.Tracer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import spock.lang.Specification

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.core.MultivaluedHashMap

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class ServerLoggingOpenTracingAdapterFilterSpec extends Specification {
	private static final Logger log = LoggerFactory.getLogger(ServerLoggingOpenTracingAdapterFilterSpec.class)
	static JaegerTracingExtractor jaegerTracer = new JaegerTracingExtractor()

	def "given i have no active span, i should get a request-id set"() {
		given: "i have a header configuration"
		  HeaderLoggingConfiguration hlc = new HeaderLoggingConfiguration(null)
		  hlc.completeConfiguration()
		and: "a clear MDC"
		  MDC.clear()
		and: "and a fake tracer"
		  Tracer tracer = [activeSpan: { -> return null }] as Tracer
		and: "a server context pass filter"
		  jaegerTracer.enabled = false
		  ServerLoggingOpenTracingAdapterFilter filter = new ServerLoggingOpenTracingAdapterFilter(hlc, tracer,
			  new NoopTracingExtractor(hlc))
		and: "a fake request"
		  ContainerRequestContext request = [getHeaders: {->
			  return new MultivaluedHashMap<String, String>()
		  }] as ContainerRequestContext
		when: "i pass in a incoming filter request"
		  JaegerTracingExtractor.enabled = false
		  filter.filter(request)
		  log.info("here lies dragons")
		then: "a request-id is set on the MDC"
		  MDC.get(TracingExtractor.REQUEST_ID) != null
	}

	def "an active span with baggage, should push certain data into MDC"() {
		given: "i have an header config"
		  HeaderLoggingConfiguration hlc = new HeaderLoggingConfiguration(null)
		  hlc.configuredPropagateHeaders = ['baggage', 'X-Header=sausage']
		  hlc.appName = 'mary'
		  hlc.completeConfiguration()
		and: "a clear MDC"
		  MDC.clear()
		and: "a fake tracer"
		  Map<String, String> baggages = ['baggage': 'luggage', 'flavour': 'soy sauce']
		  Map<String, String> tags = [:]

		  Tracer tracer = [activeSpan: { -> return [
		    context: { -> return new SpanContext(1L, 2L, 3L, (byte)0) },
			  getBaggageItem: { key ->
				  return baggages[key]
			  },
			  setTag: { String key, String val ->
				  tags[key] = val
				  return [:] as ActiveSpan // we don't use this mechanism, so its ok
			  }
		  ] as ActiveSpan }] as Tracer
		and: "a fake request"
			ContainerRequestContext request = [getHeaders: {->
				MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<String, String>()

				headers.add('X-Header', 'cumberlands')
				headers.add('flavour', 'worstershire')

				return headers
			}] as ContainerRequestContext
		and: "a server context pass filter"
		  ServerLoggingOpenTracingAdapterFilter filter = new ServerLoggingOpenTracingAdapterFilter(hlc, tracer, jaegerTracer)
		  jaegerTracer.enabled = true
		when: "i pass in a incoming request"
		  filter.filter(request)
		  log.info('does this show anything')
		then:
		  MDC.getCopyOfContextMap().size() == 6
		  MDC.get('sausage') == 'cumberlands'
		  MDC.get('baggage') == 'luggage'
		  MDC.get(TracingExtractor.REQUEST_ID) == '1'
		  MDC.get(TracingExtractor.REQUEST_SPAN) == '2'
		  MDC.get(TracingExtractor.REQUEST_PARENT_SPAN) == '3'
		  MDC.get('appName') == 'mary'
		  tags['appName'] == 'mary'
	}

  def "record processing time and close out MDC"() {
	  given: "i have put stuff in MDC"
	    MDC.clear()
	    MDC.put("hello", "there")
	  and: "have server filter"
	    jaegerTracer.enabled = false
	    ServerLoggingOpenTracingAdapterFilter filter = new ServerLoggingOpenTracingAdapterFilter(
		    null, null,
		    new NoopTracingExtractor(new HeaderLoggingConfiguration(null)))
	  and: "a request"
	    ContainerRequestContext request = [:] as ContainerRequestContext
	  and: "a response"
	    ContainerResponseContext response = [getStatus:{-> return 201}] as ContainerResponseContext
	  when: "i ask for an output server filter"
	    filter.filter(request, response)
	  then: "MDC is empty"
	    MDC.getCopyOfContextMap().size() == 0
  }
}
