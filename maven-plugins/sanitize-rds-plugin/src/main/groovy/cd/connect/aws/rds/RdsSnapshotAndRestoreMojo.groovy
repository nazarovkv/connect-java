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
	@Parameter(property = "rds-restore.dumpCommand", required = true)
	String dumpCommand;
	@Parameter(property = "rds-restore.dumpFolder")
	String dumpFolder = "dump";
	@Parameter(property = "rds-restore.schemas", required = true)
	List<String> schemas;
	@Parameter(property = "rds-restore.executeSqlCommand", required = true)
	String executeSqlCommand;
	@Parameter(property = "rds-restore.sanitizeFolder")
	String sanitizeFolder = "src/sql"
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
	@Parameter(property = "rds-restore.parallel-dump")
	boolean parallelDump = false

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
		init()

		if (!useSnapshot) {
			snapshot()
		} else {
			realSnapshotName = useSnapshot
			cleanSnapshot = false // never clean this
		}

		DBInstance db = null;

		getLog().info("Skip restore ${skipRestore}, sanitize ${skipSanitize}, useSnapshot ${useSnapshot}")

		String hostName

		if (skipRestore) {
			if (hostname) {
				hostName = hostname
			} else {
				db = rdsClone.getDatabaseInstance(database)
				cleanSanitize = false // never clean this
				hostName = (db.getEndpoint().address + ":" + db.getEndpoint().port)
			}
		} else {
			if (rdsClone.getDatabaseInstance(database + "-sanitize")) {
				getLog().info("cleaning old sanitize copy from snapshot")
				rdsClone.deleteDatabaseInstance(database + "-sanitize")
			}
			restoreSnapshot(database + "-sanitize", realSnapshotName, { boolean success, DBInstance instance ->
				db = instance
			})

			hostName = (db.getEndpoint().address + ":" + db.getEndpoint().port)
		}

		if (!skipSanitize) {
			File sanitizeFolder = new File(sanitizeFolder)
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
			File dFolder = new File(dumpFolder)
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
			rdsClone.deleteDatabaseInstance(database + "-sanitize")
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
