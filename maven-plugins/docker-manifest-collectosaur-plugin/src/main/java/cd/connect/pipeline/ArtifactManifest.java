package cd.connect.pipeline;

/**
 * Created by Richard Vowles on 20/03/18.
 */
public class ArtifactManifest {
	public String baseImageName;
	public String fullImageName;
	public String serviceName;

	public ArtifactManifest() {
	}

	private ArtifactManifest(Builder builder) {
		setBaseImageName(builder.baseImageName);
		setFullImageName(builder.fullImageName);
		setServiceName(builder.serviceName);
	}

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

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}


	public static final class Builder {
		private String baseImageName;
		private String fullImageName;
		private String serviceName;

		public Builder() {
		}

		public Builder baseImageName(String val) {
			baseImageName = val;
			return this;
		}

		public Builder fullImageName(String val) {
			fullImageName = val;
			return this;
		}

		public Builder serviceName(String val) {
			serviceName = val;
			return this;
		}

		public ArtifactManifest build() {
			return new ArtifactManifest(this);
		}
	}
}
