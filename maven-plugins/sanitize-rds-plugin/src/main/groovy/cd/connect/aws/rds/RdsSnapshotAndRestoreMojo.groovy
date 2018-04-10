package cd.connect.aws.rds

import com.amazonaws.services.rds.model.DBInstance
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
	@Parameter(property = "rds-restore.hostname")
	String hostname
	@Parameter(property = "rds-restore.dumpCommand")
	String dumpCommand;
	@Parameter(property = "rds-restore.dumpFolder")
	String dumpFolder = "dump";
	@Parameter(property = "rds-restore.schemas")
	List<String> schemas;
	@Parameter(property = "rds-restore.executeSqlCommand")
	String executeSqlCommand;
	@Parameter(property = "rds-restore.sanitizeFolder")
	String sanitizeFolder = "src/sql"
	@Parameter(property = "rds-restore.sanitizeSqls")
	List<String> sanitizeSqls;
	@Parameter(property = "rds-restore.snapshot-name-override")
	String snapshotNameOverride
	@Parameter(property = "rds-restore.skip-snapshot")
	boolean skipSnapshot
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
	@Parameter(property = "rds-restore.parallel-dump")
	boolean parallelDump = false
	@Parameter(property = "rds-restore.into-database")
	String intoDatabase
	@Parameter(property = "rds-restore.skip-rds") // this makes no sense as a property, but we can't is it in an execution if we don't have this
	boolean skip

	private String createCommand(String base, String msg, String hostName) {
		String cwd = new File('.').absolutePath

		String command = base.replace('$username', username).replace('$hostname', hostName)
		String fakeCommand = command.replace('$password', '*******')
		command = command.replace('$password', password).replace('$cwd', cwd)

		getLog().info("Using command for ${msg}: `${fakeCommand}` with schemas `${schemas}")

		return command
	}

	@Override
	void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) { return }


		getLog().info("Skip snapshot ${skipSnapshot}, restore ${skipRestore}, sanitize ${skipSanitize}, snapshotNameOverride `${snapshotNameOverride}`")

		if (skipSnapshot) {
			if (!skipRestore && !snapshotNameOverride) {
				throw new MojoFailureException("If you are going to skip snapshot but do a restore, you need to specify the snapshotNameOverride")
			}

			if (snapshotNameOverride) {
				realSnapshotName = snapshotNameOverride
				cleanSnapshot = false // never clean this
			}
		}

		if (!skipSnapshot && snapshotNameOverride) {
			throw new MojoFailureException("Snapshoting and overriding the snapshot name make no sense, if you wish to set the snapshot name, use snapshotName")
		}

		init()

		if (!skipSnapshot) {
			snapshot()
		}

		DBInstance db = null;

		String hostName

		String sanitizeName = intoDatabase ?: database + "-sanitize"

		if (skipRestore) {
			cleanSanitize = false // never clean this

			if (hostname) {
				hostName = hostname
			} else {
				db = rdsClone.getDatabaseInstance(database)
				getLog().info("db endpoint ${db.endpoint.toString()}")
				hostName = (db.getEndpoint().address + ":" + db.getEndpoint().port)
			}
		} else {
			if (rdsClone.getDatabaseInstance(sanitizeName)) {
				getLog().info("cleaning old sanitize copy from snapshot")
				rdsClone.deleteDatabaseInstance(sanitizeName, restoreWaitInMinutes, pollTimeInSeconds)
			}

			restoreSnapshot(sanitizeName, realSnapshotName, { boolean success, DBInstance instance ->
				println "instance set"
				db = instance
			})

			println "db instance? ${db}"

			getLog().info("db endpoint ${db.endpoint.toString()}")
			hostName = (db.getEndpoint().address + ":" + db.getEndpoint().port)
		}

		if (!skipSanitize) {
			File sanitizeFolder = new File(project.basedir, sanitizeFolder)
			String command = createCommand(executeSqlCommand, "sanitize sql", hostName)
				.replace('$sanitizeFolder', sanitizeFolder.absolutePath)

			sanitizeSqls.each { String sqlFile ->
				getLog().info("SQL File $sqlFile")
				String localCommand = command.replace('$sanitizeSql', sqlFile)
				Process p = splitAndKeepQuotesTogether(localCommand).execute()
				getLog().info(localCommand)
				p.waitFor()

				if (p.exitValue() != 0) {
					getLog().info("Error code ${p.exitValue()} : ${p.errorStream?.text}")
					throw new MojoFailureException("Failed to process request to sanitize")
				}
			}
		}

		if (!skipDump) {
			File dFolder = new File(project.basedir, dumpFolder)
			dFolder.mkdirs()
			String command = createCommand(dumpCommand, "db dump", hostName)
			if (!parallelDump) {
				schemas.forEach({ String schema ->
					dumpSchema(schema, command, dFolder)
				})
			} else {
				schemas.parallelStream().forEach({ String schema ->
					dumpSchema(schema, command, dFolder)
				})
			}
		}

		if (cleanSnapshot) {
			rdsClone.deleteDatabaseSnapshot(realSnapshotName)
		}

		if (cleanSanitize) {
			rdsClone.deleteDatabaseInstance(sanitizeName, 0, 0)
		}
	}

	private static List<String> splitAndKeepQuotesTogether(String cmd) {
		List<String> commands = [];

		String[] items = cmd.split(' ')
		String element = ''
		boolean inQuote = false

		items.each { String item ->
			if (inQuote) {
				element += ' ' + item.substring(0, item.length() - 1)
				if (item.endsWith("'")) {
					inQuote = false
					commands.add(element)
					element = ''
				}
			} else if (item.startsWith("'")) {
				if (item.endsWith("'")) {
					commands.add(item.substring(1, item.length() - 1))
				} else {
					inQuote = true
					if (element) { element += ' '}
					element += item.substring(1)
				}
			} else {
				commands.add(item)
			}
		}

		if (element) {
			commands.add(element)
		}

		println "returning ${commands}"

		return commands
	}

	private void dumpSchema(String schema, String command, File dFolder) {
		getLog().info("dumping $schema")
		String localCommand = command.replace('$schema', schema)
		Process p = splitAndKeepQuotesTogether(localCommand).execute()
		p.consumeProcessOutput(new FileOutputStream(new File(dFolder, "${schema}.sql")),
			new FileOutputStream(new File(dFolder, "${schema}.err")))
		p.waitFor()
		getLog().info("Exit Value ${p.exitValue()}")
	}
}
