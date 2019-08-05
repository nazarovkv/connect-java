package cd.connect.pipeline;

import java.util.List;

public class DockerManifest {
	public List<ArtifactManifest> manifest;
	public String pr;
	public String sha;
	public String originalSha;
	public String branch;

	public List<ArtifactManifest> getManifest() {
		return manifest;
	}

	public void setManifest(List<ArtifactManifest> manifest) {
		this.manifest = manifest;
	}

	public String getPr() {
		return pr;
	}

	public void setPr(String pr) {
		this.pr = pr;
	}

	public String getSha() {
		return sha;
	}

	public void setSha(String sha) {
		this.sha = sha;
	}

	public String getOriginalSha() {
		return originalSha;
	}

	public void setOriginalSha(String originalSha) {
		this.originalSha = originalSha;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}
}
