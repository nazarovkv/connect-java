package cd.connect.tracing.jersey;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate with this to bring these into spring context
 *
 * Created by Richard Vowles on 11/01/18.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({EnvironmentLoggingFilter.class, PropertyLoggingFilter.class})
public @interface EnableEnvPropertyLoggingJerseyFilters {
}
