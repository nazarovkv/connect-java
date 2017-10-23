package cd.connect.spring.jersey.log

import cd.connect.spring.jersey.JerseyLoggerPoint
import org.glassfish.jersey.logging.Constants
import spock.lang.Specification

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class JerseyLoggingMappingSpec extends Specification {
	def "rest content removed"() {
		given: "i have a remaining context containing jersey headers"
		  def headers = [:]
		  headers[Constants.REST_CONTEXT] = "burp"
		  headers[Constants.REST_STATUS_CODE] = "100"
		when: "i pass this to the jersey logging mapping"
		  def jlm = new JerseryLoggingMapping()
			jlm.map(headers, [:], [])
		then: "they are removed"
		  headers.size() == 0
	}

	def "when in log, content headers are for message"() {
		given: "i have a context that contains headers"
		  def headers = [:]
		  headers["some data"] = 20
		  String contextMessage = "becomes message"
		  headers[Constants.REST_CONTEXT] = contextMessage
		  Integer statusCode = 201
		  headers[Constants.REST_STATUS_CODE] = statusCode.toString()
		and: "i have a log that indicates it is a jersey log"
		  def log = [:]
		  log.path = JerseyLoggerPoint.LOGGER_POINT
		  String payload = "this is the message and becomes the payload"
		  log.message = payload
		when: "i pass this into the jersey logging mapper"
			def jlm = new JerseryLoggingMapping()
			jlm.map(headers, log, [])
		then: "we get the message swapped to payload"
		  log.jersey != null
		  log.jersey.payload == payload
		and: "the message becomes the context"
		  log.message == contextMessage
		and: "the status is embedded in the jersey object"
		  log.jersey.statusCode == statusCode
	}
}
