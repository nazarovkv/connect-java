package cd.connect.aws.rds

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Mojo(name = "rds-snapshot",
	defaultPhase = LifecyclePhase.NONE,
	requiresProject = false,
	requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
class RdsSnapshotMojo extends BaseSnapshotMojo {
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init()
		snapshot()
	}
}
