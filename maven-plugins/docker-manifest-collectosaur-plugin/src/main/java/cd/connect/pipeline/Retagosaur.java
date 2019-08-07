package cd.connect.pipeline;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static cd.connect.pipeline.CollectosaurMojo.checkForExternalBearerToken;

/**
 * This will retag an existing tag, copying the manifest over. It is used once all of the e2e tests pass
 * and we are ready for an environment to be tagged.
 */
@Mojo(name = "retagosaur",
	defaultPhase = LifecyclePhase.PACKAGE,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class Retagosaur extends AbstractMojo {
	// where we are looking for artifacts
	@Parameter(name = "dockerRegistry", required = true)
	private String dockerRegistry;

	// to access the registry
	@Parameter(name = "dockerRegistryBearerToken", required = true)
	private String dockerRegistryBearerToken;

	// this is the name of this deployment container, e.g /co-name/deploy
	@Parameter(name = "deployContainerImageName", required = true)
	private String deployContainerImageName;

	// so we can find a successful one. This is used by the Retagosaur as well (to retag on success)
	@Parameter(name = "targetEnvironment", required = true)
	private String targetEnvironment;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!dockerRegistry.startsWith("http")) {
			dockerRegistry = "https://" + dockerRegistry;
		}

		// to avoid it being in the logs, read it from a file and strip whitespace
		dockerRegistryBearerToken = checkForExternalBearerToken(dockerRegistryBearerToken);

		DockerUtils dockerUtils = new DockerUtils(dockerRegistry, dockerRegistryBearerToken);


	}
}
