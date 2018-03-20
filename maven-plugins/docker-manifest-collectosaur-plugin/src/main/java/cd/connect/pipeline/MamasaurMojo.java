package cd.connect.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * This is designed to be run in a reactor
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Mojo(name = "mamasaur",
	defaultPhase = LifecyclePhase.PACKAGE,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class MamasaurMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true)
	MavenProject project;

	@Parameter(defaultValue = "${project.build.directory}")
	private File projectBuildDir;

	@Parameter(defaultValue = "${project.directory}")
	private File projectDir;

	private ObjectMapper om = new ObjectMapper();

	private MojoFailureException mfe = null;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<ArtifactManifest> manifests = Collections.synchronizedList(new ArrayList<>());

		walkModels(projectDir, project.getModel(), manifests);

		File manifestFile = new File(projectBuildDir, BabysaurMojo.MANIFEST_NAME);

		projectBuildDir.mkdirs();

		try {
			om.writeValue(manifestFile, manifests);
		} catch (IOException e) {
			throw new MojoFailureException("unable to write manifest file for reactor", e);
		}
	}

	private void walkModels(File projectDir, Model project, List<ArtifactManifest> manifests) throws MojoFailureException {
		if (project.getModules() != null) {
			mfe = null;

			project.getModules().parallelStream().forEach(module -> {
				try {
					locateTargetManifest(projectDir, manifests, module);
					locateHiddenDeepReactors(projectDir, manifests, module);
				} catch (MojoFailureException e) {
					mfe = e;
				}
			});

			if (mfe != null) {
				throw mfe;
			}
		}
	}

	/**
	 * in here we are trying to determine if there is a pom.xml file in the module folder that has further
	 * modules that we need to spelunk into.
	 *
	 * @param projectDir - the reactor parent folder
	 * @param manifests - the list of manifests generated
	 * @param module - the module we have to look into
	 */
	private void locateHiddenDeepReactors(File projectDir, List<ArtifactManifest> manifests, String module)
		throws MojoFailureException {
		MavenXpp3Reader modelReader = new MavenXpp3Reader();
		File pomFile = new File(projectDir, module + "/pom.xml");

		if (pomFile.exists()) {
			getLog().info("checking module " + module + " for sub-modules.");
			try {
				walkModels(
					new File(projectDir, module),
					modelReader.read(new FileReader(pomFile)),
					manifests);
			} catch (IOException|XmlPullParserException e) {
				String msg = "Unable to parse pom.xml " + pomFile.getPath();
				getLog().error(msg);
				throw new MojoFailureException(msg, e);
			}
		} else {
			getLog().error("module exists without a pom in " + pomFile.getPath());
		}
	}

	private void locateTargetManifest(File projectBuildDir, List<ArtifactManifest> manifests, String module) throws MojoFailureException {
		File manifest = new File(projectBuildDir, module + "/target/" + BabysaurMojo.MANIFEST_NAME);

		if (manifest.exists()) {
			getLog().info("loading " + manifest.getPath());

			try {
				manifests.add(om.readValue(manifest, ArtifactManifest.class));
			} catch (IOException e) {
				getLog().error("Unable to read manifest " + manifest.getPath());
				throw new MojoFailureException("Cannot read manifest", e);
			}
		} else {
			getLog().info("No manifest in " + manifest.getPath());
		}
	}
}
