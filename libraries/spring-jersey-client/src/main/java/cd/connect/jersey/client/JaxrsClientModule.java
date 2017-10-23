package cd.connect.jersey.client;

import cd.connect.spring.Module;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
abstract public class JaxrsClientModule extends Module {

  protected String targetUrl;

  protected void target(Class<?> clazz) {
    AbstractBeanDefinition bean = BeanDefinitionBuilder.genericBeanDefinition(JaxrsFactoryBean.class).setScope("singleton").getRawBeanDefinition();

    bean.getPropertyValues().add("targetClass", clazz);
    bean.getPropertyValues().add("targetUrl", targetUrl);

    this.beanDefinitionRegistry.registerBeanDefinition(clazz.getName(), bean);
  }

  protected void target(Class<?>... clazzes) {
    for (Class<?> clazz : clazzes) {
      target(clazz);
    }
  }
}
