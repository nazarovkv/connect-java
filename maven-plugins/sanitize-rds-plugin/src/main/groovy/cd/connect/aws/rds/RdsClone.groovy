package cd.connect.aws.rds

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.rds.AmazonRDS
import com.amazonaws.services.rds.AmazonRDSClientBuilder
import com.amazonaws.services.rds.model.CreateDBSnapshotRequest
import com.amazonaws.services.rds.model.DBInstance
import com.amazonaws.services.rds.model.DBInstanceNotFoundException
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import groovy.transform.CompileStatic

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Richard Vowles on 16/02/18.
 */
@CompileStatic
class RdsClone {
	AmazonRDS rdsClient;
	AmazonSQS sqsClient;
	//String rdsTopic = 'global-rds-event-topic'
	String rdsQueue = 'global-rds-event-queue'
	List<Closure> outstandingActions = []
	AtomicBoolean stop = new AtomicBoolean()
	AtomicBoolean started = new AtomicBoolean()
	RdsConsumer rdsConsumer

	void initialize(String profile = null) {
		AWSCredentialsProviderChain chain = new DefaultAWSCredentialsProviderChain()

		if (profile != null) {
			chain = new AWSCredentialsProviderChain(Arrays.asList(new ProfileCredentialsProvider(profile)))
		}

		rdsClient = AmazonRDSClientBuilder.standard().withCredentials(chain).build()
		sqsClient = AmazonSQSAsyncClientBuilder.standard().withCredentials(chain).build()

		rdsConsumer = new RdsConsumer(sqsClient, rdsQueue, stop)
		rdsConsumer.start()
	}

	void initializeAndCleanQueue(String qName = null) {
		if (qName) {
			this.rdsQueue = qName
		}


	}

	void snapshotDatabase(String database) {
		String snapshotName = database + "-" + System.currentTimeMillis()
		long start = System.currentTimeMillis()
		CreateDBSnapshotRequest snap = new CreateDBSnapshotRequest()
		  .withDBInstanceIdentifier(database)
			.withDBSnapshotIdentifier(snapshotName)
		long end = System.currentTimeMillis()

		println "snapshot ${snapshotName} created in ${(end-start)}ms";
	}

	List<String> discoverSnapshots(String database) {
		DescribeDBSnapshotsResult snapshots = rdsClient.describeDBSnapshots(new DescribeDBSnapshotsRequest().withDBInstanceIdentifier(database))

		return snapshots.DBSnapshots.collect({ it.getDBSnapshotIdentifier() })
	}

	void createDatabaseInstanceFromSnapshot(String database, String snapshot, Closure completed) {
		try {
			rdsClient.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(database))
			throw new RuntimeException("Database already exists, please rename before restoring.")
		} catch (DBInstanceNotFoundException dinfe) {

		}

		long end = System.currentTimeMillis()

		long start = System.currentTimeMillis()
		DBInstance instance = rdsClient.restoreDBInstanceFromDBSnapshot(new RestoreDBInstanceFromDBSnapshotRequest()
			.withDBInstanceIdentifier(database)
		  .withDBSnapshotIdentifier(snapshot))

		if (completed) {
			rdsConsumer.addListener { RdsEvent event ->
				if (event.source == RdsEvent.SourceType.INSTANCE && event.event == )
			}
			outstandingActions.add({ evt ->

			})
		}

		println "created ${database} from instance ${snapshot} in ${end-start}ms"
		println instance
	}

	void deleteDatabaseInstance(String database) {
		DBInstance instance = rdsClient.deleteDBInstance(new DeleteDBInstanceRequest().withDBInstanceIdentifier(database))
		println "instance deleting."
	}

	String databaseStatus(String database) {
		return rdsClient.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(database)).DBInstances.first().DBInstanceStatus
	}

	public static void main(String[] args) {
		def rds = new RdsClone();
		rds.initialize()
//		rds.createDatabaseInstanceFromSnapshot("fishandchips", "productioncd-master-mysql-sanitized-1482538522")
		println "status ${rds.databaseStatus('fishandchips')}"

		Thread.sleep(5000)
		println "shutting down q"
		rds.stop.set(true)
		Thread.sleep(5000)
		println "finished"
	}
}
