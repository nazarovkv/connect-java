package cd.connect.jersey.common;

import cd.connect.opentracing.LoggingSpanTracer;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.util.GlobalTracer;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class TracingConfiguration implements Feature {
  // override this if you wish to make it different
  protected Tracer getTracer() {
    return  new io.jaegertracing.Configuration(System.getProperty("app.name", "local-app"))
      // We need to get a builder so that we can directly inject the
      // reporter instance.
      .withReporter(Configuration.ReporterConfiguration.fromEnv())
      .withSampler(Configuration.SamplerConfiguration.fromEnv().withType("const").withParam(1)).getTracer();
  }

  @Override
  public boolean configure(FeatureContext ctx) {
    GlobalTracer.register(new LoggingSpanTracer(getTracer()));

    // i don't like that these require the HttpServletRequest - we don't have one!
    ctx.register(ClientTracingFeature.class);
    ctx.register(ServerTracingDynamicFeature.class);
    ctx.register(SpanWorkaroundFilter.class);
    
    return true;
  }
}
