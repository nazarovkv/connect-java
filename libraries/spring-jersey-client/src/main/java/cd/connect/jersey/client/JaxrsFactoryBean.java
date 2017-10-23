package cd.connect.jersey.client;

import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.springframework.beans.factory.FactoryBean;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class JaxrsFactoryBean implements FactoryBean {
  private String targetUrl;
  private Class<?> targetClass;

  private final JaxrsClient jaxrsClient;

  @Inject
  public JaxrsFactoryBean(JaxrsClient jaxrsClient) {
    this.jaxrsClient = jaxrsClient;
  }

  public String getTargetUrl() {
    return targetUrl;
  }

  public void setTargetUrl(String targetUrl) {
    this.targetUrl = targetUrl;
  }

  public Class<?> getTargetClass() {
    return targetClass;
  }

  public void setTargetClass(Class<?> targetClass) {
    this.targetClass = targetClass;
  }

  @Override
  public Object getObject() throws Exception {
    WebTarget target = jaxrsClient.getClient().target(targetUrl);

    return WebResourceFactory.newResource(targetClass, target);
  }

  @Override
  public Class<?> getObjectType() {
    return targetClass;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
