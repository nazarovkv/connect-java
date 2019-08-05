package cd.connect.pipeline


import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class DockerUtils {
	private final String dockerRegistryBase;
	// https://registry-1.docker.io/
	private final String dockerRegistryBearerToken;
	private final ObjectMapper mapper = new ObjectMapper()

	DockerUtils(String dockerRegistryBase, String dockerRegistryBearerToken) {
		this.dockerRegistryBase = dockerRegistryBase + "/v2"
		this.dockerRegistryBearerToken = dockerRegistryBearerToken
	}

	String latestReleaseTag(String imageName, String targetEnv) {
		// Return the latest ReleaseTag which was produced by submit-queue
		def registryReleaseTagsApi = "${dockerRegistryBase}/${imageName}/tags/list?n=999"
		println "Searching " + registryReleaseTagsApi + " for latest release tag ${targetEnv}"

		def tagsRequest = new URL(registryReleaseTagsApi).openConnection() as HttpURLConnection
		tagsRequest.setRequestProperty("Authorization", basicAuthCreds)
		tagsRequest.setRequestProperty("Accept", "application/json")
		tagsRequest.connect()

		String body = tagsRequest.content.toString()
		println("Body is ${body} - response code is ${tagsRequest.responseCode}")

		if (body == null) {
			return null
		}

		def tags = new JsonSlurper().parseText(body)['tags']

		//filter for tags that are in the correct format of timestamp.build.project-env.cluster.deploy.timestamp
		//e.g 1540501501119.7.ci.nonprod.deploy.1540502359372

		def filteredTags = tags.findAll { it ==~ /(\d+)\.(\d+)\.${targetEnv}\.(\w+)\.(\w+)\.(\d+)/ }
		filteredTags.sort()
		filteredTags = filteredTags.reverse()

		println "FILTER RETURNED LATEST " + filteredTags[0].toString()

		return filteredTags[0]
	}

	/**
	 * Here we are just interested in previous list of successful artifacts, not the other data
	 * which is specific to that build.
	 *
	 * @param imageName - the docker image, e.g. featurehub/mr
	 * @param environment that tag was created in - e.g. ci
	 * @return - the manifest
	 */
	List<ArtifactManifest> getLatestArtifactManifest(String imageName, String suffix) {
		String tag = latestReleaseTag(imageName, environment)

		if (tag != null) {
			DockerManifest manifest = latestReleaseManifest(imageName, tag)
			if (manifest) {
				 return manifest.manifest
			}
		}

		return null
	}

	DockerManifest latestReleaseManifest(String imageName, String latestReleaseTagVersion) {
		def json = new JsonSlurper()
		def registryReleaseManifestApi = "${dockerRegistryBase}/${imageName}/manifests/${latestReleaseTagVersion}"
		println "ManifestAPI URL found: " + registryReleaseManifestApi
		def dockerManifestRequest = new URL(registryReleaseManifestApi).openConnection()
		dockerManifestRequest.setRequestProperty("Authorization", "Bearer $dockerRegistryBearerToken")
		dockerManifestRequest.setRequestProperty("Accept", "application/json")
		HttpURLConnection urlConnection = dockerManifestRequest.openConnection() as HttpURLConnection
		urlConnection.connect()
		println("response code is ${urlConnection.responseCode}")
		if (urlConnection.responseCode == 200) {
			def dockerManifestResponse = urlConnection.content?.toString()

			println("content is $dockerManifestResponse");

			if (dockerManifestResponse != null) {
				println(JsonOutput.prettyPrint(dockerManifestResponse));
			}

			def dockerManifest = json.parseText(dockerManifestResponse)

			return mapper.readValue(dockerManifest.history[0].v1Compatibility.toString(), DockerManifest)
		} else {
			return null
		}
	}
}
