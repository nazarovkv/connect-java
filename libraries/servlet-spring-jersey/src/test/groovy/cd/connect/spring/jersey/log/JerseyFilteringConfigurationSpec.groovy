package cd.connect.spring.jersey.log

import spock.lang.Specification

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class JerseyFilteringConfigurationSpec extends Specification {
	def "exclude complete url configuration and use cases work"() {
		given: "when we define a bunch of urls to be excluded"
		when: "i create and initiatze the filter configuration"
		then: "i am excluded from these urls"
		and: "not these urls"
	}

	def "exclude payload url configuration and use cases work"() {
		given: "when we define a bunch of urls to be excluded"
		when: "i create and initialize the filter configuration"
		then: "i am excluded from these urls"
		and: "not these urls"
	}


}
