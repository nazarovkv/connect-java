package cd.connect.aws.rds

import com.amazonaws.services.rds.model.DBInstance
import com.sun.javafx.tools.packager.Param
import groovy.transform.CompileStatic
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Mojo(name = "rds-sanitize",
	defaultPhase = LifecyclePhase.NONE,
	requiresProject = false,
	requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
@CompileStatic
class RdsSnapshotAndRestoreMojo extends BaseSnapshotMojo {
	@Parameter(property = "rds-restore.dumpCommand", required = true)
	String dumpCommand;
	@Parameter(property = "rds-restore.schemas", required = true)
	List<String> schemas;
	@Parameter(property = "rds-restore.executeSqlCommand", required = true)
	String executeSqlCommand;
	@Parameter(property = "rds-restore.sanitizeSqls", required = true)
	List<String> sanitizeSqls;
	@Parameter(property = "rds-restore.use-snapshot")
	String useSnapshot
	@Parameter(property = "rds-restore.skip-restore")
	boolean skipRestore = false
	@Parameter(property = "rds-restore.skip-sanitize")
	boolean skipSanitize = false
	@Parameter(property = "rds-restore.skip-dump")
	boolean skipDump = false
	@Parameter(property = "rds-restore.clean-sanitize")
	boolean cleanSanitize = false
	@Parameter(property = "rds-restore.clean-snapshot")
	boolean cleanSnapshot = false

	private String createCommand(String base, String msg, String hostName) {
		String command = base.replace('$username', username).replace('$hostname', hostName)
		String fakeCommand = command.replace('$password', '*******')
		command = command.replace('$password', password)

		getLog().info("Using command for ${msg}: `${fakeCommand}` with schemas `${schemas}")

		return command
	}

	@Override
	void execute() throws MojoExecutionException, MojoFailureException {
		init()

		if (!useSnapshot) {
			snapshot()
		} else {
			realSnapshotName = useSnapshot
			cleanSnapshot = false // never clean this
		}

		DBInstance db;

		if (skipRestore) {
			db = rdsClone.getDatabaseInstance(database)
			cleanSanitize = false // never clean this
		} else {
			if (rdsClone.getDatabaseInstance(database + "-sanitize")) {
				getLog().info("cleaning old sanitize copy from snapshot")
				rdsClone.deleteDatabaseInstance(database + "-sanitize")
			}
			restoreSnapshot(database + "-sanitize", realSnapshotName, { boolean success, DBInstance instance ->
				db = instance
			})
		}

		String hostName = db.getEndpoint() + ":" + db.getDbInstancePort()

		if (!skipSanitize) {
			String command = createCommand(executeSqlCommand, "sanitize sql", hostName)

			sanitizeSqls.each { String sqlFile ->
				getLog().info("SQL File $sqlFile")
				Process p = command.replace('$sqlFile', sqlFile).execute()
				getLog().info(p.text)
			}
		}

		if (!skipDump) {
			String command = createCommand(dumpCommand, "db dump", hostName)
			schemas.each { String schema ->
				getLog().info("Schema $schema")
				Process p = command.replace('$schema', schema).execute()
				getLog().info(p.text)
			}
		}

		if (cleanSnapshot) {
			rdsClone.deleteDatabaseSnapshot(realSnapshotName)
		}

		if (cleanSanitize) {
			rdsClone.deleteDatabaseInstance(database + "-sanitize")
		}
	}
}
