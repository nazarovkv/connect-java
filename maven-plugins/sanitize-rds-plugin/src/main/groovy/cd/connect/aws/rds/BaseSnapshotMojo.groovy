package cd.connect.aws.rds

import groovy.transform.CompileStatic
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Parameter

@CompileStatic
abstract public class BaseSnapshotMojo extends AbstractMojo {

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

	protected RdsClone rdsClone;
	protected String realSnapshotName;

	protected void init() {
		rdsClone = new RdsClone()
	}
	protected String snapshot() throws MojoFailureException {
		realSnapshotName = rdsClone.snapshotDatabase(database, snapshotWaitInMinutes, pollTimeInSeconds, snapshotName)
		if (!realSnapshotName) {
			throw new MojoFailureException("Unable to create snapshot within timeframe")
		}
	}

	protected void restoreSnapshot(String dbName, String snapshotName, CreateInstanceResult createInstanceResult) {
		rdsClone.createDatabaseInstanceFromSnapshot(dbName, snapshotName, restoreWaitInMinutes, pollTimeInSeconds, createInstanceResult)
	}
}

