package cd.connect.prescan

import spock.lang.Specification

class PrescanGeneratorMojoSpec extends Specification{

	void "We scan for our artifacts"(){
		given: "we have a scanner and a classloader"
			ClassLoader loader = getClass().getClassLoader()
			PrescanGeneratorMojo mojo = new PrescanGeneratorMojo() {
				@Override
				protected File getOutputDirectoryFile() {
					return new File( getClass().getResource('/' ).getFile() )
				}
			}
			mojo.projectBuildDir = new File( getClass().getResource('/' ).getFile() )

		when: "we perform a scan"
			Set<String> found = mojo.scan( loader )

		then: "we expect to find stuff"
			found.find { 'webxml=jar:file:/WEB-INF/web.xml' == it }
			found.find { 'resource=jar:file:/META-INF/resources/' == it }
			!found.find { 'resource=jar:file:/META-INF/resources/empty.txt' == it }
			found.find { 'fragment=jar:file:/META-INF/web-fragment.xml' == it }
			found.find { 'webxml=jar:file:/META-INF/resources/WEB-INF/web.xml' == it }
	}

	void "We scan our jars for artifacts"(){
		given: "we have a scanner and a classloader"
			ClassLoader loader = new URLClassLoader( asURLArray( [ '/jars/binks.jar', '/jars/fragment.jar' ] ), (ClassLoader)null )
			PrescanGeneratorMojo mojo = new PrescanGeneratorMojo()  {
				@Override
				protected File getOutputDirectoryFile() {
					return new File( getClass().getResource('/' ).getFile() )
				}
			}
			mojo.jarpath = "lib"

		when: "we perform a scan"
			Set<String> found = mojo.scan(loader )

		then: "we expect to find stuff"
			found.size() == 8
			found.find { 'webxml=jar:file:/lib/binks.jar!/WEB-INF/web.xml' == it }
			found.find { 'fragment=jar:file:/lib/fragment.jar!/META-INF/web-fragment.xml' == it }
			!found.find { 'resource=jar:file:/lib/fragment.jar!/META-INF/resources/' == it } 		// fragment resources should not be here as there is nothing in it
	}

	private static URL[] asURLArray( List<String> strings ) {
		return strings.collect { getClass().getResource( it ).toURI().toURL() }.toArray() as URL[]
	}

}
