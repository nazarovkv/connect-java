package cd.connect.jersey.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JerseyExceptionMapper implements ExceptionMapper<Exception> {
	private static final Logger log = LoggerFactory.getLogger(JerseyExceptionMapper.class);
	@Override
	public Response toResponse(Exception exception) {
		if (exception instanceof WebApplicationException) {
      Response response = ((WebApplicationException) exception).getResponse();
      if (response.getStatus() >= 500) { // special callout to all our 5xx in the house.
        log.error("Failed jersey request", exception);
      }

      return response;
		}
		
		log.error("Failed jersey request", exception);
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
	}
}
