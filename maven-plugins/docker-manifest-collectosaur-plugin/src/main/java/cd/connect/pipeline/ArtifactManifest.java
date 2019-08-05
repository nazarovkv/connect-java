package cd.connect.pipeline;

/**
 * Created by Richard Vowles on 20/03/18.
 */
public class ArtifactManifest {
	public String baseImageName;
	public String fullImageName;
	public String module;

	public String getBaseImageName() {
		return baseImageName;
	}

	public void setBaseImageName(String baseImageName) {
		this.baseImageName = baseImageName;
	}

	public String getFullImageName() {
		return fullImageName;
	}

	public void setFullImageName(String fullImageName) {
		this.fullImageName = fullImageName;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}
}
