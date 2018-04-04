package cd.connect.aws.rds

import groovy.transform.CompileStatic
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "rds-clone",
	defaultPhase = LifecyclePhase.NONE,
	requiresProject = false,
	requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
@CompileStatic
public class RdsSnapshotMojo extends AbstractMojo {

	@Parameter(property = "rds-clone.database", required = true)
	String database
	@Parameter(property = "rds-clone.username", required = true)
	String username
	@Parameter(property = "rds-clone.password", required = true)
	String password
	@Parameter(property = "rds-clone.snapshot-wait")
	int snapshotWaitInMinutes = 20
	@Parameter(property = "rds-clone.restore-wait")
	int restoreWaitInMinutes = 20
	@Parameter(property = "rds-clone.poll-interval")
	int pollTimeInSeconds = 20
	@Parameter(property = "rds-clone.snapshotName")
	String snapshotName

	private RdsClone rdsClone;
	private String realSnapshotName;

	protected void init() {
		rdsClone = new RdsClone()
	}

	protected String snapshot() {
		realSnapshotName = rdsClone.snapshotDatabase(database, snapshotWaitInMinutes, pollTimeInSeconds, snapshotName)
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init()
		snapshot()

		if (!realSnapshotName) {
			throw new MojoFailureException("Unable to create snapshot within timeframe")
		}
	}
}

