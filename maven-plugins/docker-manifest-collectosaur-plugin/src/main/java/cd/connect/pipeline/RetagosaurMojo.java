package cd.connect.pipeline;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static cd.connect.pipeline.DeployosaurMojo.checkForExternalBearerToken;

/**
 * This will retag an existing tag, copying the manifest over. It is used once all of the e2e tests pass
 * and we are ready for an environment to be tagged.
 *
 * It can be used one of two ways
 *
 * (a) - using a mergeSha, which will add a new manifest with that sha so
 * it can be found by display tools (like the Connect Dashboard) to track back exactly where in the history it was
 * merged/squashed/rebased.
 * (b) - without a merge sha, which will cause it to create a .deploy.TIMESTAMP manifest which is intended for
 * the e2e run to indicate this is a "golden image".
 */
@Mojo(name = "retagosaur",
	defaultPhase = LifecyclePhase.PACKAGE,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class RetagosaurMojo extends AbstractMojo {
	// where we are looking for artifacts
	@Parameter(name = "dockerRegistry", required = true)
	private String dockerRegistry;

	// to access the registry
	@Parameter(name = "dockerRegistryBearerToken", required = true)
	private String dockerRegistryBearerToken;

	// this is the name of this deployment container, e.g /co-name/deploy
	@Parameter(name = "deployContainerImageName", required = true)
	private String deployContainerImageName;

	@Parameter(name = "deployContainerTag", required = true)
	private String deployContainerTag;

	@Parameter(name = "targetNamespace", required = true)
	private String targetNamespace;
	@Parameter(name = "targetCluster", required = true)
	private String targetCluster;

	@Parameter(name = "mergeSha", required = false)
	private String mergeSha;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!dockerRegistry.startsWith("http")) {
			dockerRegistry = "https://" + dockerRegistry;
		}

		// to avoid it being in the logs, read it from a file and strip whitespace
		dockerRegistryBearerToken = checkForExternalBearerToken(dockerRegistryBearerToken);

		DockerUtils dockerUtils = new DockerUtils(dockerRegistry, dockerRegistryBearerToken, info -> getLog().info(info));

		try {
			dockerUtils.tagReleaseContainer(deployContainerImageName, deployContainerTag,
				deployContainerTag + "." + targetNamespace + "." + targetCluster, mergeSha);
		} catch (Exception e) {
			throw new MojoFailureException("Unable to re-tag container", e);
		}
	}
}
