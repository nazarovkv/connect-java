package cd.connect.aws.rds

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.rds.AmazonRDS
import com.amazonaws.services.rds.AmazonRDSClientBuilder
import com.amazonaws.services.rds.model.*
import groovy.transform.CompileStatic

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Richard Vowles on 16/02/18.
 */
@CompileStatic
class RdsClone {
	AmazonRDS rdsClient;

	void initialize(String profile = null) {
		AWSCredentialsProviderChain chain = new DefaultAWSCredentialsProviderChain()

		if (profile != null) {
			chain = new AWSCredentialsProviderChain(Arrays.asList(new ProfileCredentialsProvider(profile)))
		}

		rdsClient = AmazonRDSClientBuilder.standard().withCredentials(chain).build()
	}

	String snapshotDatabase(String database, int waitPeriodInMinutes, int waitPeriodPollTimeInSeconds, String snapshotOverride) {
		String snapshotName = snapshotOverride ?: database + "-" + System.currentTimeMillis()
		long start = System.currentTimeMillis()
		CreateDBSnapshotRequest snap = new CreateDBSnapshotRequest()
		  .withDBInstanceIdentifier(database)
			.withDBSnapshotIdentifier(snapshotName)
		long end = System.currentTimeMillis()

		println "snapshot ${snapshotName} created in ${(end-start)}ms";

		boolean success = waitFor(waitPeriodInMinutes, waitPeriodPollTimeInSeconds, { ->
			return "available".equals(snapshotStatus(snapshotName, database))
		});

		return success ? snapshotName : null;
	}

	String snapshotStatus(String snapshot, String database) {
		return rdsClient.describeDBSnapshots(new DescribeDBSnapshotsRequest()
			.withDBInstanceIdentifier(database)
		  .withDBSnapshotIdentifier(snapshot))?.getDBSnapshots()?.first().getStatus()
	}

	List<String> discoverSnapshots(String database) {
		DescribeDBSnapshotsResult snapshots = rdsClient.describeDBSnapshots(new DescribeDBSnapshotsRequest()
			.withDBInstanceIdentifier(database)
		)

		return snapshots.DBSnapshots.collect({ it.getDBSnapshotIdentifier() })
	}

	void createDatabaseInstanceFromSnapshot(String database, String snapshot, int waitPeriodInMinutes, int waitPeriodPollTimeInSeconds, CreateInstanceResult completed) {
		try {
			rdsClient.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(database))
			throw new RuntimeException("Database already exists, please rename before restoring.")
		} catch (DBInstanceNotFoundException dinfe) {}

		long end = System.currentTimeMillis()

		long start = System.currentTimeMillis()
		DBInstance instance = rdsClient.restoreDBInstanceFromDBSnapshot(new RestoreDBInstanceFromDBSnapshotRequest()
			.withDBInstanceIdentifier(database)
		  .withDBSnapshotIdentifier(snapshot))

		println "created ${database} from instance ${snapshot} in ${end-start}ms"

		boolean success = waitFor(waitPeriodInMinutes, waitPeriodPollTimeInSeconds, { ->
			return "available".equals(databaseStatus(database))
		});

		if (completed) {
			completed.result(success, instance)
		}
	}

	// defaults to success = true if we aren't waiting for it
	protected boolean waitFor(int waitPeriodInMinutes, int waitPeriodPollTimeInSeconds, Closure criteria) {
		boolean success = true;

		int waitPeriodInSeconds = waitPeriodInMinutes * 60

		while (waitPeriodPollTimeInSeconds > 0 && waitPeriodInSeconds > 0) {
			success = criteria()

			if (success) {
				break
			}

			waitPeriodInSeconds -= waitPeriodPollTimeInSeconds

			println "${waitPeriodInSeconds} seconds left..."
		}

		return success
	}

	void deleteDatabaseInstance(String database) {
		DBInstance instance = rdsClient.deleteDBInstance(new DeleteDBInstanceRequest().withDBInstanceIdentifier(database))
	}

	String databaseStatus(String database) {
		return getDatabaseInstance(database)?.DBInstanceStatus
	}
	

	DBInstance getDatabaseInstance(String database) {
		return rdsClient.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(database)).DBInstances?.first()
	}

	void deleteDatabaseSnapshot(String snapshot) {
		rdsClient.deleteDBSnapshot(new DeleteDBSnapshotRequest().withDBSnapshotIdentifier(snapshot))
	}
}
