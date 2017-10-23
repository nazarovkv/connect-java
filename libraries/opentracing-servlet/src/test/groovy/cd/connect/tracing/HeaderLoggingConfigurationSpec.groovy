package cd.connect.tracing

import spock.lang.Specification

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class HeaderLoggingConfigurationSpec extends Specification {
	def "config will pull desired logging fields from injected config"() {
		given: "we have two sources"
		  HeaderLoggingConfigurationSource hlc1 = [getHeaderLoggingConfig: {-> return ['sausage=cumberlands']}] as HeaderLoggingConfigurationSource
		  HeaderLoggingConfigurationSource hlc2 = [getHeaderLoggingConfig: {-> return ['steak=pepper']}] as HeaderLoggingConfigurationSource
		and: "we create the config"
		  HeaderLoggingConfiguration hlc = new HeaderLoggingConfiguration([hlc1, hlc2])
		and: "add a local sticky-config"
		  hlc.configuredPropagateHeaders = ['session-id', 'trace-id']
		when: "i complete configuration"
		  hlc.completeConfiguration()
		then: "we should have a list of valid headers"
		  hlc.acceptHeaders.size() == 4
		  hlc.acceptHeaders.containsAll(['sausage', 'steak', 'session-id', 'trace-id'])
		and: "a map of valid headers"
		  hlc.headerToLoggingMapping.size() == 4
		  hlc.headerToLoggingMapping['sausage'] == 'cumberlands'
		  hlc.headerToLoggingMapping['steak'] == 'pepper'
		  hlc.headerToLoggingMapping['session-id'] == 'session-id'
		  hlc.headerToLoggingMapping['trace-id'] == 'trace-id'
	}

	def "app name is exposed correctly"() {
		when: "i create the config with no sources"
		  HeaderLoggingConfiguration hlc = new HeaderLoggingConfiguration(null)
		and: "i set the app-name"
		  hlc.appName = 'fred'
		  hlc.completeConfiguration()
		then:
		  hlc.getAppName() == 'fred'
	}
}
