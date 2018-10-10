package cd.connect.spring.jersey;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiOperation;
import io.swagger.jaxrs.config.JaxrsScanner;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.BaseApiListingResource;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Set;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Path("/swagger.{type:json|yaml}")
public class ConnectSwaggerApiResource extends BaseApiListingResource {
	@Context
	ServletContext context;

	protected boolean initialized = false;

	public class ConnectJaxrsSwagger implements JaxrsScanner {
		private final Application application;

		public ConnectJaxrsSwagger(Application application) {
			this.application = application;
		}

		@Override
		public Set<Class<?>> classesFromContext(Application app, ServletConfig sc) {
			return (Set<Class<?>>)app.getProperties().get(JerseyApplication.RESOURCE_INTERFACE_LIST);
		}

		@Override
		public Set<Class<?>> classes() {
			return (Set<Class<?>>)application.getProperties().get(JerseyApplication.RESOURCE_INTERFACE_LIST);
		}

		@Override
		public boolean getPrettyPrint() {
			return true;
		}

		@Override
		public void setPrettyPrint(boolean b) {

		}
	}

	@GET
	@Produces({MediaType.APPLICATION_JSON, "application/yaml"})
	@ApiOperation(value = "The swagger definition in either JSON or YAML", hidden = true)
	public Response getListing(
		@Context Application app,
		@Context ServletConfig sc,
		@Context HttpHeaders headers,
		@Context UriInfo uriInfo,
		@PathParam("type") String type) throws JsonProcessingException {
		if (!initialized) {
			sc.getServletContext().setAttribute(SwaggerContextService.SCANNER_ID_PREFIX + SwaggerContextService.SCANNER_ID_KEY, new ConnectJaxrsSwagger(app));
			initialized = true;
		}
		
		if (StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")) {
			return getListingYamlResponse(app, context, sc, headers, uriInfo);
		} else {
			return getListingJsonResponse(app, context, sc, headers, uriInfo);
		}
	}
}
