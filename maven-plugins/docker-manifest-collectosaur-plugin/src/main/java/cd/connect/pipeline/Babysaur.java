package cd.connect.pipeline;

import org.apache.maven.plugins.annotations.Parameter;

public class Babysaur {
	@Parameter(name = "baseImageName", required = true)
	private String baseImageName;
	@Parameter(name = "fullImageName", required = true)
	private String fullImageName;

	@Parameter(name = "serviceName", required = false)
	private String serviceName;

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
}
