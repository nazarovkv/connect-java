package cd.connect.tracing.extractors

import spock.lang.Specification

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class TracingJsonLogEnhancerSpec extends Specification {
	def "tracer removes jaeger entries and turns them into longs"() {
		given: "i have a jaeger tracer extractor"
		  JaegerTracingExtractor jte = new JaegerTracingExtractor()
		and: "a set of logs"
		  Map<String, String> context = [:]
			context[TracingExtractor.REQUEST_PARENT_SPAN] = '3'
			context[TracingExtractor.REQUEST_SPAN] = '2'
			context[TracingExtractor.REQUEST_ID] = '1'
		when: "i process them"
		  Map<String, Object> logs = [:]
		  new TracingJsonLogEnhancer().map(context, logs, [])
		then: "they are longs"
		  logs.size() == 3
		  logs[TracingExtractor.REQUEST_PARENT_SPAN] == 3L
		  logs[TracingExtractor.REQUEST_SPAN] == 2L
		  logs[TracingExtractor.REQUEST_ID] == 1L

	}
}
