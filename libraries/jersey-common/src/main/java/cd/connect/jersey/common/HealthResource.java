package cd.connect.jersey.common;

import cd.connect.lifecycle.ApplicationLifecycleManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/health")
public class HealthResource {
	@GET
	@Path("/liveness")
	public Response liveness() {
		return ApplicationLifecycleManager.isAlive() ? Response.ok().build() : Response.serverError().build();
	}

	@GET
	@Path("/readiness")
	public Response readyness() {
		return ApplicationLifecycleManager.isReady() ? Response.ok().build() : Response.serverError().build();
	}
}

