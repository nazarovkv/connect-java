package cd.connect.spring.jersey;

import cd.connect.jackson.JacksonObjectProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Created by Richard Vowles on 25/09/17.
 */
@Provider
public class JacksonContextProvider implements ContextResolver<ObjectMapper> {
	@Override
	public ObjectMapper getContext(Class<?> aClass) {
		return JacksonObjectProvider.mapper;
	}
}
