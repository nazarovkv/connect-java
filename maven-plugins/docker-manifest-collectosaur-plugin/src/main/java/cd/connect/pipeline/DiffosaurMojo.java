package cd.connect.pipeline;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * The purpose of this Mojo is simply to run "git diff" against master and compare it to the modules
 * in your main reactor pom. It will output a file that can then be used to passed to Maven so it will only
 * build the artifacts that have changed (using -pl)
 */
@Mojo(name = "diffosaur",
	defaultPhase = LifecyclePhase.INITIALIZE,
	requiresDependencyCollection = ResolutionScope.NONE,
	requiresDependencyResolution = ResolutionScope.NONE,
	requiresProject = false,
	threadSafe = true)
public class DiffosaurMojo extends AbstractMojo {
	@Parameter(name = "diffAgainst")
	private String diffAgainst = "master";

	// assume the current directory
	@Parameter(name = "pomLocation", property = "diffosaur.pomLocation")
	private String pomLocation;

	@Parameter(name = "outputFilePrefix")
	private String outputFile = "diffosaur";

	@Parameter(name = "gitDiff", property = "diffosaur.gitDiff")
	private String gitDiff = "git diff --name-only ..$diffAgainst";

	// git is expected on PATH

	static class Detector implements Runnable {
		private final InputStream inputStream;
		private final Map<String, Boolean> modules;

		Detector(InputStream inputStream, Map<String, Boolean> modules) {
			this.inputStream = inputStream;
			this.modules = modules;
		}

		@Override
		public void run() {

			new BufferedReader(new InputStreamReader(inputStream)).lines()
				.forEach(l -> {
					int pos = l.indexOf(File.separatorChar);
					if (pos != -1) {
						String prefix = l.substring(0, pos);
						if (modules.get(prefix) != null) {
							modules.put(prefix, Boolean.TRUE);
						}
					}
				});
		}
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Process diffProcess = new ProcessBuilder()
				.command(gitDiff.replace("$diffAgainst", diffAgainst).split(" "))
				.start();

			Detector detector = new Detector(diffProcess.getInputStream(), readPom());

			final Future<?> submit = Executors.newSingleThreadExecutor().submit(detector);

			int exitCode = diffProcess.waitFor();

			submit.get();

			if (exitCode == -1) {
				throw new MojoExecutionException("Process diff failed");
			}

			String val = detector.modules.entrySet().stream().filter((m) -> m.getValue() == Boolean.TRUE)
				.map(Map.Entry::getKey).collect(Collectors.joining(","));

			getLog().info("Detected updates for " + val);

			FileUtils.write(new File(outputFile + ".bat"), "SET MAVEN_DIFF=" + val, Charset.defaultCharset());
			FileUtils.write(new File(outputFile + ".sh"), "export MAVEN_DIFF=\"" + val + "\"", Charset.defaultCharset());
			FileUtils.write(new File(outputFile + ".txt"), val, Charset.defaultCharset());
			// now we know _nothing_ about the order of dependencies, so we just bundle them all up in a string and put them out.
		} catch (IOException | InterruptedException | ExecutionException e) {
			throw new MojoExecutionException("Cannot process request", e);
		}
	}

	private Map<String, Boolean> readPom() throws MojoFailureException {
		MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();

		Map<String, Boolean> modules = new HashMap<>();

		try {
			FileReader fileReader = new FileReader(pomLocation == null ? "pom.xml" : pomLocation);
			Model model = xpp3Reader.read(fileReader);
			model.getModules().forEach(m -> modules.put(m, Boolean.FALSE));

			getLog().info("Discovered modules: " + modules.keySet().stream().sorted().collect(Collectors.joining(",")));
		} catch (IOException | XmlPullParserException e) {
			throw new MojoFailureException("Cannot find pom.xml to read for modules");
		}

		if (modules.size() == 0) {
			getLog().error("There are no modules in the pom, this will always fail.");
		}

		return modules;
	}
}
