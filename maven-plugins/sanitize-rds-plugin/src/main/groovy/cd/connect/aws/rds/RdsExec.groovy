package cd.connect.aws.rds


class RdsExec {
	static int execSqlFile(String filename, String username, String password, String host) {
		Process process = "mysql -u ${username} -p${password} -h ${host} <${filename}".execute()

		println process.text

		return process.exitValue()
	}

	static int dumpSchema(String schema, String outputFolder, String username, String password, String host) {
		Process process = "mysqldump --single-transaction --quick --no-autocommit --no-create-info --extended-insert=false -h  -u $username -p$password $schema > ${outputFolder}/${schema}.sql".execute()

		println process.text

		return process.exitValue()
	}
}
