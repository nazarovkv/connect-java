package cd.connect.jersey.common;

import io.opentracing.contrib.jaxrs2.internal.CastUtils;
import io.opentracing.contrib.jaxrs2.internal.SpanWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import static io.opentracing.contrib.jaxrs2.internal.SpanWrapper.PROPERTY_NAME;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@ConstrainedTo(RuntimeType.SERVER)
@Priority(Integer.MIN_VALUE)
public class SpanWorkaroundFilter implements ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(SpanWorkaroundFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        SpanWrapper spanWrapper = CastUtils.cast(requestContext.getProperty(PROPERTY_NAME), SpanWrapper.class);
        if (spanWrapper != null) {
            log.info("finishing span");
            spanWrapper.finish();
            requestContext.removeProperty(PROPERTY_NAME);
        }
    }
}
