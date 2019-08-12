package cd.connect.pipeline


import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.apache.tools.ant.taskdefs.condition.Http

class DockerUtils {
	private final String dockerRegistryBase; // e.g https://registry-1.docker.io/ or gcr.io
	private final String dockerRegistryBearerToken;
	private final ObjectMapper mapper = new ObjectMapper()
	private final LogCallback logCallback

	DockerUtils(String dockerRegistryBase, String dockerRegistryBearerToken, LogCallback logCallback) {
		this.dockerRegistryBase = dockerRegistryBase + "/v2"
		this.dockerRegistryBearerToken = dockerRegistryBearerToken
		this.logCallback = logCallback
	}

	//filter for tags that are in the correct format of timestamp.build.project-env.cluster.deploy.timestamp
	//e.g 1540501501119.7.ci.nonprod.deploy.1540502359372

	String latestReleaseTag(String imageName, String targetEnv) {
		def filteredTags = null

		logCallback.info("Looking for tags: ${dockerRegistryBase}/${imageName}/tags/list")

		// Return the latest ReleaseTag which was produced by submit-queue
		get("${dockerRegistryBase}/${imageName}/tags/list?n=999", ["Accept": "application/json"]) { body, code ->
			List<String> tags = new JsonSlurper().parseText(body)['tags'] as List<String>
//			println tags.collect({"\"$it\""}).join(",")
//			tags.removeAll { it ==~ /(\d+)\.(\d+)\.${targetEnv}\.(?=\S*['-])([a-zA-Z'-]+)\.final\.(\d+)/ }
//			println tags
			filteredTags = tags.findAll { it ==~ /(\d+)\.(\d+)\.${targetEnv}\.(?=\S*['-])([a-zA-Z'-]+)\.deploy\.(\d+)/ }
			filteredTags.sort()
			filteredTags = filteredTags.reverse()
		}

		if (filteredTags) {
//			println "FILTER RETURNED LATEST " + filteredTags[0].toString()
			logCallback.info("Merging with manifest from ${filteredTags[0]}")
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

	interface GetResult {
		void result(String data, int code);
	}

	@CompileStatic
	void get(String url, Map<String, String> headers, GetResult result) {
		HttpURLConnection conn = new URL(url).openConnection() as HttpURLConnection
		headers.each { String key, String value ->
			conn.setRequestProperty(key, value)
		}

		if (!headers['Authorization']) {
			conn.setRequestProperty('Authorization', "Bearer $dockerRegistryBearerToken")
		}

		conn.connect()

		if (conn.responseCode != 200) {
			throw new RuntimeException("Failed get ${conn.responseCode} : ${conn.inputStream.text}")
		}

		result.result(conn.inputStream.text, conn.responseCode)
	}

	void put(String url, Map<String, String> headers, String data) {
		HttpURLConnection conn = new URL(url).openConnection() as HttpURLConnection
		headers.each { String key, String value ->
			conn.setRequestProperty(key, value)
		}
		conn.setDoOutput(true)
		conn.setRequestMethod('PUT')


		if (!headers['Authorization']) {
			conn.setRequestProperty('Authorization', "Bearer $dockerRegistryBearerToken")
		}

		conn.connect()
		conn.outputStream.write(data.bytes)
		conn.outputStream.flush()
		conn.outputStream.close()

		if (conn.responseCode != 200) {
			throw new RuntimeException("Failed put ${url} with data ${data} - ${conn.inputStream.text}")
		}
	}

	DockerManifest latestReleaseManifest(String imageName, String latestReleaseTagVersion) {
		def json = new JsonSlurper()
		String configSha = null

		get("${dockerRegistryBase}/${imageName}/manifests/${latestReleaseTagVersion}",
			["Accept": "application/vnd.docker.distribution.manifest.v2+json"]) { content, code ->

			configSha = json.parseText(content).config.digest
		}

		if (configSha == null) {
			throw new RuntimeException("Unable to get config sha");
		}

		DockerManifest manifest = null

		get("${dockerRegistryBase}/${imageName}/blobs/${configSha}", ["Accept":"application/json" ]) { configContent, configCode ->
			def dockerManifest = json.parseText(configContent)
			manifest = mapper.readValue(dockerManifest.config.Labels.buildManifest.toString(), DockerManifest)
		}

		if (manifest == null) {
			throw new RuntimeException("Was unable to get manifest!")
		}

		return manifest
	}

	void tagReleaseContainer(String imageName, String buildTag, String releaseTag, String mergeSha) {
		String manifestType = 'application/vnd.docker.distribution.manifest.v2+json'
		String buildManifestUrl = "${dockerRegistryBase}/${imageName}/manifests/${buildTag}"
		String releaseManifestUrl = "${dockerRegistryBase}/${imageName}/manifests/${releaseTag}"

		String releaseManifest = null

		Map<String, String> manifestHeaders = ['Accept': 'application/vnd.docker.distribution.manifest.v2+json']
		get(buildManifestUrl, manifestHeaders) { String data, int code ->
			releaseManifest = data
		}

		if (releaseManifest == null) {
			throw new RuntimeException("Failed to tag ${releaseManifestUrl} - no release manifest to copy from")
		}

		manifestHeaders = ['Content-Type': manifestType]

		if (mergeSha) {
			// this is the same container that was deployed into the current environment after a test run but is tagged "final" with the sha for the merge
			// it should never be picked up as a consideration for future manifest merging
			String releaseManifestShaUrl = "${dockerRegistryBase}/${imageName}/manifests/${releaseTag}.final.${mergeSha}"
			logCallback.info("Tagging for merge sha: ${releaseManifestShaUrl}")

			put(releaseManifestShaUrl, manifestHeaders, releaseManifest)
		} else {
			releaseManifestUrl += ".deploy.${new Date().getTime()}"
			logCallback.info("Updating release manifest for ${releaseManifestUrl}")
			put(releaseManifestUrl, manifestHeaders, releaseManifest)
		}
	}
}
