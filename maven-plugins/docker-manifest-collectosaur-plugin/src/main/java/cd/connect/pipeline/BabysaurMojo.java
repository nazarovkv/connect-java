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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Richard Vowles on 20/03/18.
 */
@Mojo(name = "babysaur",
	defaultPhase = LifecyclePhase.PACKAGE,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class BabysaurMojo extends AbstractMojo {
  public static final String MANIFEST_NAME = "connect-manifest.json";

	@Parameter(defaultValue = "${project}", readonly = true)
	MavenProject project;

	@Parameter(defaultValue = "${project.build.directory}")
	private File projectBuildDir;

	@Parameter(name = "baseImageName", required = true)
	private String baseImageName;
	@Parameter(name = "fullImageName", required = true)
	private String fullImageName;

	@Parameter(name = "serviceName", required = false)
	private String serviceName;

	// in case we wish to write the same module name out again under different names
	@Parameter(name = "extras")
	private List<Babysaur> extras;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ObjectMapper om = new ObjectMapper();

		try {
		  projectBuildDir.mkdirs();

			FileWriter fw = new FileWriter(new File(projectBuildDir, MANIFEST_NAME));
			List<ArtifactManifest> manifests = new ArrayList<>();

			manifests.add(new ArtifactManifest.Builder()
				.baseImageName(baseImageName)
				.fullImageName(fullImageName)
				.serviceName(serviceName == null ? project.getArtifactId() : serviceName).build());

			if (extras != null) {
				extras.forEach(b -> {
					manifests.add(new ArtifactManifest.Builder()
						.baseImageName(b.getBaseImageName())
						.fullImageName(b.getFullImageName())
						.serviceName(b.getServiceName() == null ? project.getArtifactId() : b.getServiceName()).build());
				});
			}

			fw.write(om.writeValueAsString(manifests));
			fw.flush();
			fw.close();

		} catch (IOException e) {
			getLog().error("failed", e);
			throw new MojoFailureException("failed", e);
		}
	}
}
