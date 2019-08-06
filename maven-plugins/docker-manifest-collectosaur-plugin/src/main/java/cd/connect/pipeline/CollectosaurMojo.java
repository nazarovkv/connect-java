package cd.connect.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
 * 1540501501119.7.ci.nonprod.deploy.1540502359372
 * when they succeed they look like this:
 * 1540501501119.7.ci.nonprod.deploy.1540502359372.final.mergeSha
 * (where mergeSha is the sha returned from the repository when something is merged)
 */
@Mojo(name = "collectosaur",
	defaultPhase = LifecyclePhase.PACKAGE,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class CollectosaurMojo extends AbstractMojo {
	private static final String COLLECTOSAUR_MANIFEST = "collectosaurManifest";
	private static final String COLLECTOSAUR_BRANCH = "collectosaurBranch";
	private static final String COLLECTOSAUR_SHA = "collectosaurSha";
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

	// this is the name of this deployment container
	@Parameter(name = "deploymentContainerName", required = false)
	private String deploymentContainerName;

	// so we can find a successful one. This is used by the Retagosaur as well (to retag on success)
	@Parameter(name = "retagOnSuccessSuffix", required = false)
	private String retagOnSuccessSuffix;

	@Parameter(name = "sha")
	private String sha;
	@Parameter(name = "pullRequest")
	private String pullRequest;
	@Parameter(name = "originalSha")
	private String originalSha;
	@Parameter(name = "branch")
	private String branch;

	ObjectMapper mapper = new ObjectMapper();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<ArtifactManifest> manifestList = new ArrayList<>();

		if (dockerRegistry != null && dockerRegistryBearerToken != null) {
			if (!dockerRegistry.startsWith("https://")) {
				dockerRegistry = "https://" + dockerRegistry;
			}

			DockerUtils dockerUtils = new DockerUtils(dockerRegistry, dockerRegistryBearerToken);
			// first we check in the registry and see if there is a tagged previous successful build.
			// if there isn't - that is ok, we assume this is the very first one.
			manifestList = dockerUtils.getLatestArtifactManifest(deploymentContainerName, retagOnSuccessSuffix);
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
			getLog().info(String.format("manifest - module %s - image %s", m.module, m.fullImageName));
		});

		DockerManifest newManifest = new DockerManifest();
		newManifest.manifest = finalManifest;
		newManifest.sha = sha;
		newManifest.pr = pullRequest;
		newManifest.originalSha = originalSha;
		newManifest.branch = branch;

		writeSystemProperties(newManifest);

		writeJsonFile();
		writeYamlFile(newManifest);
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
			sb.append(String.format("%s:\n  image:\n    prefix: '%s'\n    tag: '%s'\n", m.module, parts[0], parts[1]));
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

	private void writeJsonFile() throws MojoFailureException {
		if (outputJsonManifestFile != null) {

			// ensure the folders exist
			File oFile = new File(outputJsonManifestFile);

			if (!oFile.getParentFile().exists()) {
				oFile.getParentFile().mkdirs();
			}

			try (FileWriter fw = new FileWriter(outputJsonManifestFile)) {
				IOUtils.write(System.getProperty(COLLECTOSAUR_MANIFEST), fw);
			} catch (IOException iex) {
				throw new MojoFailureException("Cannot write output json file", iex);
			}
		}
	}

	private void writeSystemProperties(DockerManifest newManifest) throws MojoFailureException {
		try {
			System.setProperty(COLLECTOSAUR_MANIFEST, mapper.writeValueAsString(newManifest));
		} catch (JsonProcessingException e) {
			throw new MojoFailureException("Unable to write manifest json to system property");
		}

		System.setProperty(COLLECTOSAUR_BRANCH, branch == null ? "master" : branch);

		if (sha != null) {
			System.setProperty(COLLECTOSAUR_SHA, sha);
		}
	}


	private List<ArtifactManifest> mergeManifests(List<ArtifactManifest> manifestList, List<ArtifactManifest> inputManifests) {
		Map<String, ArtifactManifest> existing = inputManifests.stream().collect(Collectors.toMap(ArtifactManifest::getModule, Function.identity()));

		List<ArtifactManifest> newList = new ArrayList<>(inputManifests);
		manifestList.forEach(m -> {
			if (existing.get(m.module) == null) {
				newList.add(m);
			}
		});

		return newList;
	}
}
