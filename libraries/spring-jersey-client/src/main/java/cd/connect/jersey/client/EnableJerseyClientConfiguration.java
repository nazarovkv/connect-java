package cd.connect.jersey.client;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Richard Vowles on 12/10/17.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({FilteringClientConfigurer.class, DefaultClientConfigurer.class, JaxrsClientManager.class})
public @interface EnableJerseyClientConfiguration {
}
