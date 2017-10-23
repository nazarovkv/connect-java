package cd.connect.tracing.extractors;

import cd.connect.tracing.HeaderLoggingConfiguration;
import io.opentracing.NoopTracer;
import io.opentracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Configuration
public class TracingExtractorRegistration {
	@Bean
	public TracingExtractor getTracingExtractor(Tracer tracer, HeaderLoggingConfiguration hlConfig) {
		if (tracer instanceof NoopTracer) {
			return new NoopTracingExtractor(hlConfig);
		} else {
			return new JaegerTracingExtractor();
		}
	}
}
