package cd.connect.spring.jersey;

/**
 * This forms the basis of an application. You don't have to use it, it is just a simple kick-start
 * that you can descend from and just annotate extra @Import's on your own class. It expects to be loaded
 * as a Listener.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */

import cd.connect.spring.jersey.log.JerseyFilteringConfiguration;
import cd.connect.spring.servlet.ServletModuleRegistration;
import com.bluetrainsoftware.common.config.EnableStickyConfiguration;
import com.bluetrainsoftware.common.config.PreStartRepository;
import io.prometheus.client.hotspot.DefaultExports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Arrays;


@Configuration
@EnableStickyConfiguration
@Import({JerseyFilteringConfiguration.class, ServletModuleRegistration.class, DefaultServerConfigurer.class, FilteringServerConfigurer.class})
public class BaseWebApplication implements ServletContextListener {
	protected static final Logger log = LoggerFactory.getLogger(BaseWebApplication.class);
	protected AnnotationConfigWebApplicationContext context;
	protected ServletContext servletContext;
	public static final String APPL_CTX = "spring.applicationcontext";

	public BaseWebApplication() {
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		this.servletContext = sce.getServletContext();

		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(sce.getServletContext());

		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);

		String registrationClass = System.getProperty("connect.registrationClasses");

		/**
		 * Allow configuration to override which class is used for its annotation based configuration.
		 * This would normally be this class instance, but to ensure you can modualrize your projects and bring them
		 * together and separate them easily, this allows you to override it.
		 */
		if (registrationClass != null) {
			Arrays.stream(registrationClass.split(",")).map(String::trim).filter(s -> s.length() > 0).forEach(s -> {
					try {
						context.register(Class.forName(s));
					} catch (ClassNotFoundException e) {
						log.error("Unable to find registration class `{}`", s);
						throw new RuntimeException(e);
					}
				}
			);
		} else {
			context.register(this.getClass());
		}

		registerOtherModules();

		// initialize prometheus
		DefaultExports.initialize();

		log.info("refreshing now");
		context.refresh();
		log.info("refresh complete, starting pre-start");

		context.getBean(PreStartRepository.class).start();
		log.info("pre-start complete, starting webserver.");

		// now tell the servlet context about the refreshed context
		sce.getServletContext().setAttribute(APPL_CTX, context);
	}

	protected void registerOtherModules() {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
}

