package cd.connect.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Richard Vowles on 20/03/18.
 */
@Mojo(name = "babysaur",
	defaultPhase = LifecyclePhase.PACKAGE,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class CollectosaurPluginMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true)
	MavenProject project;

	@Parameter(defaultValue = "${project.build.directory}")
	private File projectBuildDir;

	@Parameter(name = "babysaur.baseImageName", required = true)
	private String baseImageName;
	@Parameter(name = "babysaur.fullImageName", required = true)
	private String fullImageName;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ObjectMapper om = new ObjectMapper();

		try {
			FileWriter fw = new FileWriter(new File(projectBuildDir, "connect-manifest.json"));
			ArtifactManifest am = new ArtifactManifest();

			am.baseImageName = baseImageName;
			am.fullImageName = fullImageName;

			fw.write(om.writeValueAsString(am));
			fw.flush();
			fw.close();

		} catch (IOException e) {
			getLog().error("failed", e);
			throw new MojoFailureException("failed", e);
		}
	}
}
