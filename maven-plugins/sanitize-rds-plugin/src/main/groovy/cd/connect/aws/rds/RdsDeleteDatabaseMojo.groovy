package cd.connect.aws.rds

import groovy.transform.CompileStatic
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 * Created by Richard Vowles on 24/07/18.
 */
@Mojo(name = "rds-delete",
	defaultPhase = LifecyclePhase.NONE,
	requiresProject = false,
	requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
@CompileStatic
class RdsDeleteDatabaseMojo extends AbstractMojo {
	@Parameter(property = "rds-clone.database", required = true)
	String database
	@Parameter(property = "rds-clone.aws-profile")
	String awsProfile
	@Parameter(property = "rds-restore.skip-rds") // this makes no sense as a property, but we can't is it in an execution if we don't have this
	boolean skip

	protected RdsClone rdsClone;

	@Override
	void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return
		}

		rdsClone = new RdsClone()
		rdsClone.initialize(awsProfile)

		getLog().info("Shutting down ${database}")
		rdsClone.deleteDatabase(database)
	}
}
