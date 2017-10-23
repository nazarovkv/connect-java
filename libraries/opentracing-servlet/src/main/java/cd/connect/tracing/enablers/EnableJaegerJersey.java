package cd.connect.tracing.enablers;

import cd.connect.tracing.HeaderLoggingConfiguration;
import cd.connect.tracing.extractors.JaegerTracingExtractor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this to wire the two required classes for Jaeger and Jersey.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({HeaderLoggingConfiguration.class, JaegerTracingExtractor.class})
public @interface EnableJaegerJersey {
}
