package cd.connect.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This goal is designed to take the input of the build and:
 *
 * (a) fill it in from the docker registry (i.e. collect the last green build's manifest
 * and merge it with this one)
 * (b) output a yaml manifest for this deployment container to use
 * (c) output a json manifest that can be attached as metadata for the next phase (a) if this container works
 * (d) write a shell file that contains an environment variable referring to this deployment container
 * that the build process can use to deploy it so it can run the e2e tests.
 *
 * Our build tags for the deploy container look like this:
 * 1540501501119.7
 * when they succeed they look like this:
 * 1540501501119.7.ci.nonprod.deploy.1540502359372.final.mergeSha
 * (where mergeSha is the sha returned from the repository when something is merged. This is only used for
 * diagnostic tools as labels cannot be updated once set in place)
 * or
 * 1540501501119.7.ci.nonprod.deploy.1540502359372
 * (where the mergeSha is not provided, this is the tag of the promotable deployment image)
 */
@Mojo(name = "deployosaur",
	defaultPhase = LifecyclePhase.PACKAGE,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class DeployosaurMojo extends AbstractMojo {
	private static final String COLLECTOSAUR_MANIFEST = "collectosaurManifest";
	private static final String COLLECTOSAUR_BRANCH = "collectosaurBranch";
	private static final String COLLECTOSAUR_SHA = "collectosaurSha";
	private static final String COLLECTOSAUR_TIMESTAMP = "collectosaurTimestamp";
	private static final String COLLECTOSAUR_TAG = "collectosaurTag";


	@Parameter( defaultValue = "${session}", required = true, readonly = true )
	private MavenSession session;

	// the location of the mamasaur file
	@Parameter(name = "inputManifestFile", required = true)
	private File inputManifestFile;

	// where we will write the yaml manifest file for helm
	@Parameter(name = "outputYamlManifestFile", required = true)
	private String outputYamlManifestFile;

	// where we will write the manifest file
	@Parameter(name = "outputJsonManifestFile", required = true)
	private String outputJsonManifestFile;

	// where we are looking for artifacts
	@Parameter(name = "dockerRegistry", required = false)
	private String dockerRegistry;

	// to access the registry
	@Parameter(name = "dockerRegistryBearerToken", required = false)
	private String dockerRegistryBearerToken;

	// this is the name of this deployment container, e.g /co-name/deploy
	@Parameter(name = "deployContainerImageName", required = false)
	private String deployContainerImageName;

	// this is the file to which we write the tag name that was used. This allows us to pass it on to
	// the retagosaur in a file.
	@Parameter(name = "deployContainerTagOutputFile")
	private String deployContainerTagOutputFile;

	// we use the collectosaurTimestamp . buildNumber to generate the tag and store it in the file
	@Parameter(name = "buildNumber")
	private String buildNumber;

	// so we can find a successful one. This is used by the Retagosaur as well (to retag on success)
	// e.g. ci and nonprod, ci and dev-cluster
	@Parameter(name = "targetNamespace")
	private String targetNamespace;
	@Parameter(name = "targetCluster")
	private String targetCluster;

	@Parameter(name = "sha")
	private String sha;
	@Parameter(name = "pullRequest")
	private String pullRequest;
	@Parameter(name = "branch")
	private String branch;
	// if the branch name matches this branch name, we ignore previous Docker images for deployment
	// and just use what is here. e.g. master
	@Parameter(name = "goldenBranch")
	private String goldenBranch;

	ObjectMapper mapper = new ObjectMapper();

	public static String checkForExternalBearerToken(String bearerToken) throws MojoFailureException {

		if (bearerToken.startsWith("@")) {
			File f = new File(bearerToken.substring(1));
			try {
				bearerToken = StringUtils.strip(FileUtils.readFileToString(f, Charset.defaultCharset()), "\r\n \t");
			} catch (IOException e) {
				throw new MojoFailureException("Unable to read docker bearer token from file " + f.getAbsolutePath(), e);
			}
		}

		return bearerToken;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<ArtifactManifest> manifestList = new ArrayList<>();

		// only go ask Docker for previous deploy manifest if we are not on our special branch (e.g. master)
		if (goldenBranch == null || !goldenBranch.equals(branch)) {
			if (dockerRegistry != null && dockerRegistryBearerToken != null && targetCluster != null && targetNamespace != null) {
				if (!dockerRegistry.startsWith("http")) {
					dockerRegistry = "https://" + dockerRegistry;
				}

				// to avoid it being in the logs, read it from a file and strip whitespace
				dockerRegistryBearerToken = checkForExternalBearerToken(dockerRegistryBearerToken);

				DockerUtils dockerUtils = new DockerUtils(dockerRegistry, dockerRegistryBearerToken, info -> getLog().info(info));
				// first we check in the registry and see if there is a tagged previous successful build.
				// if there isn't - that is ok, we assume this is the very first one.
				manifestList = dockerUtils.getLatestArtifactManifest(deployContainerImageName, targetNamespace);
			} else if (dockerRegistryBearerToken != null || dockerRegistry != null || targetCluster != null || targetNamespace != null) {
				throw new MojoExecutionException("If using a docker registry, you must " +
					"specify all three of dockerRegistryBearerToken, dockerRegistry, targetEnvironment");
			}
		} else {
			getLog().info(String.format("On golden branch %s so ignoring previous builds.", branch));
		}

		List<ArtifactManifest> inputManifests = null;

		try {
				inputManifests = mapper.readValue(inputManifestFile, new TypeReference<List<ArtifactManifest>>() {
			});
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read file " + inputManifestFile.toString());
		}

		List<ArtifactManifest> finalManifest;
		if (manifestList != null && manifestList.size() > 0) {
			getLog().info("Merging with previous green build.");
			finalManifest = mergeManifests(manifestList, inputManifests);
		} else {
			getLog().info("No previous manifest discovered, going with build.");
			finalManifest = inputManifests;
		}

		finalManifest.forEach(m -> {
			getLog().info(String.format("manifest - module %s - image %s", m.serviceName, m.fullImageName));
		});

		DockerManifest newManifest = new DockerManifest();
		newManifest.manifest = finalManifest;
		newManifest.sha = sha;
		newManifest.pr = pullRequest;
		newManifest.branch = branch;

		String currentTimestamp = System.currentTimeMillis() + "";
		writeSystemProperties(newManifest, currentTimestamp);

		writeJsonFile(newManifest);
		writeYamlFile(newManifest);
		writeBuildTag(currentTimestamp);
	}

	private void writeBuildTag(String currentTimestamp) throws MojoFailureException {
		if (buildNumber != null && deployContainerTagOutputFile != null) {
			try {
				FileUtils.write(new File(deployContainerTagOutputFile), currentTimestamp + "." + buildNumber, Charset.defaultCharset());
			} catch (IOException e) {
				throw new MojoFailureException("Unable to write to file " + deployContainerTagOutputFile, e);
			}
		}
	}

	// don't really want to include snakeyaml
	private void writeYamlFile(DockerManifest newManifest) throws MojoFailureException {
		StringBuilder sb = new StringBuilder();

		for (ArtifactManifest m : newManifest.manifest) {
			String[] parts = m.fullImageName.split(":");
			if (parts.length != 2) {
				getLog().error("Image " + m.fullImageName + " is not valid, it has no tag!");
				throw new MojoFailureException("Invalid image name " + m.fullImageName);
			}
			sb.append(String.format("%s:\n  image:\n    prefix: '%s'\n    tag: '%s'\n", m.serviceName, parts[0], parts[1]));
		}

		// ensure the folders exist
		File oFile = new File(outputYamlManifestFile);

		if (!oFile.getParentFile().exists()) {
			oFile.getParentFile().mkdirs();
		}

		try (FileWriter fw = new FileWriter(outputYamlManifestFile)) {
			IOUtils.write(sb.toString(), fw);
		} catch (IOException iex) {
			throw new MojoFailureException("Cannot write output json file", iex);
		}
	}

	private void writeJsonFile(DockerManifest newManifest) throws MojoFailureException {
		if (outputJsonManifestFile != null) {

			// ensure the folders exist
			File oFile = new File(outputJsonManifestFile);

			if (!oFile.getParentFile().exists()) {
				oFile.getParentFile().mkdirs();
			}

			try {
				FileUtils.write(new File(outputJsonManifestFile), mapper.writeValueAsString(newManifest), Charset.defaultCharset());
			} catch (IOException e) {
				throw new MojoFailureException("Unable to write manifest json to system property", e);
			}
		}
	}

	private void writeSystemProperties(DockerManifest newManifest, String currentTimestamp) throws MojoFailureException {
		for (MavenProject project : session.getProjectDependencyGraph().getSortedProjects()) {
			try {
				project.getProperties().setProperty(COLLECTOSAUR_MANIFEST, mapper.writeValueAsString(newManifest));
			} catch (JsonProcessingException e) {
				throw new MojoFailureException("Unable to write manifest json to system property", e);
			}

			project.getProperties().setProperty(COLLECTOSAUR_BRANCH, branch == null ? "master" : branch);

			if (sha != null) {
				project.getProperties().setProperty(COLLECTOSAUR_SHA, sha);
			}

			project.getProperties().setProperty(COLLECTOSAUR_TIMESTAMP, currentTimestamp);
		}
	}


	private List<ArtifactManifest> mergeManifests(List<ArtifactManifest> manifestList, List<ArtifactManifest> inputManifests) {
		Map<String, ArtifactManifest> existing = inputManifests.stream().collect(Collectors.toMap(ArtifactManifest::getServiceName, Function.identity()));

		List<ArtifactManifest> newList = new ArrayList<>(inputManifests);
		manifestList.forEach(m -> {
			if (existing.get(m.serviceName) == null) {
				newList.add(m);
			}
		});

		return newList;
	}
}
