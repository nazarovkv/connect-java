package cd.connect.war

import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import spock.lang.Specification

class PreScannedConfigurationSpec extends Specification {

	def "Only have a web.xml in the root"() {
		given: "we have a configuration scanner and some resources"
			System.setProperty( WebAppRunner.WEBAPP_PRE_SCANNED_RESOURCE_NAME, '/web_inf_webXML')
			PreScannedConfiguration cfg = new PreScannedConfiguration()

			WebAppContext context = new WebAppContext( classLoader: this.class.classLoader )

		when: "we scan"
			cfg.preConfigure( context )

		then:
			context.metaData.webXml.getResource() == Resource.newResource(cfg.applicationRoot + 'WEB-INF/web.xml')
			context.baseResource == Resource.newResource(cfg.applicationRoot)
			context.getAttribute( PreScannedConfiguration.RESOURCE_URLS ).first() == Resource.newResource(cfg.applicationRoot)
	}

	def "Only have a web.xml in META-INF/resources"() {
		given: "we have a configuration scanner and some resources"
			System.setProperty( WebAppRunner.WEBAPP_PRE_SCANNED_RESOURCE_NAME, '/meta_inf_webXML')
			PreScannedConfiguration cfg = new PreScannedConfiguration()
			WebAppContext context = new WebAppContext( classLoader: this.class.classLoader )

		when: "we scan"
			cfg.preConfigure( context )

		then:
			context.metaData.webXml.getResource() == Resource.newResource(cfg.applicationRoot + 'META-INF/resources/WEB-INF/web.xml')
			context.baseResource == Resource.newResource(cfg.applicationRoot + 'META-INF/resources/')
			context.getAttribute( PreScannedConfiguration.RESOURCE_URLS ).first() == Resource.newResource(cfg.applicationRoot + 'META-INF/resources/')
	}

	def "web.xml in both root and in META-INF/resources"() {
		given: "we have a configuration scanner and some resources"
			System.setProperty( WebAppRunner.WEBAPP_PRE_SCANNED_RESOURCE_NAME, '/both_webXML')
			PreScannedConfiguration cfg = new PreScannedConfiguration()
			WebAppContext context = new WebAppContext()

		when: "we scan"
			cfg.preConfigure( context )

		then:
			context.metaData.webXml.getResource() == Resource.newResource(cfg.applicationRoot + 'WEB-INF/web.xml')
			context.baseResource == Resource.newResource(cfg.applicationRoot)
			context.getAttribute( PreScannedConfiguration.RESOURCE_URLS ).first() == Resource.newResource(cfg.applicationRoot)
			context.getAttribute( PreScannedConfiguration.RESOURCE_URLS ).last() == Resource.newResource(cfg.applicationRoot + 'META-INF/resources/')
	}

	def "we can connect with darth jarjar"() {
		given: "we have a configuration scanner and some resources"
			System.setProperty( WebAppRunner.WEBAPP_PRE_SCANNED_RESOURCE_NAME, '/jarjar_binks')
			PreScannedConfiguration cfg = new PreScannedConfiguration()
			WebAppContext context = new WebAppContext( classLoader: this.class.classLoader )

		when: "we scan"
			cfg.preConfigure( context )

		then:
			context.metaData.webXml.getResource() == Resource.newResource("jar:${cfg.applicationRoot}jars/binks.jar!/WEB-INF/web.xml")
			context.baseResource == Resource.newResource("jar:${cfg.applicationRoot}jars/binks.jar!/")
			context.getAttribute( PreScannedConfiguration.RESOURCE_URLS ).first() == Resource.newResource("jar:${cfg.applicationRoot}jars/binks.jar!/")
	}

	def "web fragment"() {
		given: "we have a configuration scanner and some resources"
			System.setProperty( WebAppRunner.WEBAPP_PRE_SCANNED_RESOURCE_NAME, '/web_fragmentXML')
			PreScannedConfiguration cfg = new PreScannedConfiguration()
			WebAppContext context = new WebAppContext()

		when: "we scan"
			cfg.preConfigure( context )

		then:
			context.metaData.webXml.getResource() == Resource.newResource(cfg.applicationRoot + 'WEB-INF/web.xml')
			context.baseResource == Resource.newResource(cfg.applicationRoot)
			context.metaData.webInfJars.first() == Resource.newResource("jar:${cfg.applicationRoot}jars/fragment.jar!/")
			context.metaData.fragments.first().name == 'fragmented'
			context.getAttribute( PreScannedConfiguration.RESOURCE_URLS ).last() == Resource.newResource("jar:${cfg.applicationRoot}jars/fragment.jar!/META-INF/resources/")
	}


}
