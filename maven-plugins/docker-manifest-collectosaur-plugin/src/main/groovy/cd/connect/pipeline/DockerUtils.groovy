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
//		println "Searching " + registryReleaseTagsApi + " for latest release tag ${targetEnv}"

		def tagsRequest = new URL(registryReleaseTagsApi).openConnection() as HttpURLConnection
		tagsRequest.setRequestProperty("Authorization", "Bearer " + dockerRegistryBearerToken)
		tagsRequest.setRequestProperty("Accept", "application/json")
		tagsRequest.connect()

		String body = tagsRequest.content.text
//		println("Body is ${body} - response code is ${tagsRequest.responseCode}")

		if (body == null || tagsRequest.responseCode != 200) {
			return null
		}

		def tags = new JsonSlurper().parseText(body)['tags']

//		println("found tags $tags")

		//filter for tags that are in the correct format of timestamp.build.project-env.cluster.deploy.timestamp
		//e.g 1540501501119.7.ci.nonprod.deploy.1540502359372

		def filteredTags = tags.findAll { it ==~ /(\d+)\.(\d+)\.${targetEnv}\.(?=\S*['-])([a-zA-Z'-]+).(\w+)\.(\d+)/ }
		filteredTags.sort()
		filteredTags = filteredTags.reverse()

		if (filteredTags) {
//			println "FILTER RETURNED LATEST " + filteredTags[0].toString()
			return filteredTags[0]
		} else {
//			println "no matching tags"
			return null
		}
	}

	/**
	 * Here we are just interested in previous list of successful artifacts, not the other data
	 * which is specific to that build.
	 *
	 * @param imageName - the docker image, e.g. featurehub/mr
	 * @param environment that tag was created in - e.g. ci
	 * @return - the manifest
	 */
	List<ArtifactManifest> getLatestArtifactManifest(String imageName, String environment) {
		String tag = latestReleaseTag(imageName, environment)

		if (tag != null) {
			DockerManifest manifest = latestReleaseManifest(imageName, tag)
			if (manifest) {
				 return manifest.manifest
			}
		}

		return []
	}

	DockerManifest latestReleaseManifest(String imageName, String latestReleaseTagVersion) {
		def json = new JsonSlurper()
		def registryReleaseManifestApi = "${dockerRegistryBase}/${imageName}/manifests/${latestReleaseTagVersion}"
//		println "ManifestAPI URL found: " + registryReleaseManifestApi
		def dockerManifestRequest = new URL(registryReleaseManifestApi).openConnection()  as HttpURLConnection
		dockerManifestRequest.setRequestProperty("Authorization", "Bearer $dockerRegistryBearerToken")
		dockerManifestRequest.setRequestProperty("Accept", "application/vnd.docker.distribution.manifest.v2+json")
		dockerManifestRequest.connect()
//		println("response code is ${dockerManifestRequest.responseCode}")
		def dockerManifestResponse = dockerManifestRequest.content?.text
//		println("data is $dockerManifestResponse")

		if (dockerManifestRequest.responseCode == 200) {

//			println("content is $dockerManifestResponse");

			if (dockerManifestResponse != null) {
				println(JsonOutput.prettyPrint(dockerManifestResponse));
			}

			def dockerManifest = json.parseText(dockerManifestResponse)
			def configSha = dockerManifest.config.digest

			registryReleaseManifestApi = "${dockerRegistryBase}/${imageName}/blobs/${configSha}"
//			println "Config ManifestAPI URL found: " + registryReleaseManifestApi
			dockerManifestRequest = new URL(registryReleaseManifestApi).openConnection()  as HttpURLConnection
			dockerManifestRequest.setRequestProperty("Authorization", "Bearer $dockerRegistryBearerToken")
			dockerManifestRequest.setRequestProperty("Accept", "application/json")
			dockerManifestRequest.connect()

			dockerManifestResponse = dockerManifestRequest.content?.text
//			println("data is $dockerManifestResponse ${dockerManifestRequest.responseCode}")
			dockerManifest = json.parseText(dockerManifestResponse)

			return mapper.readValue(dockerManifest.config.Labels.buildManifest.toString(), DockerManifest)
		} else {
			return null
		}
	}
}
