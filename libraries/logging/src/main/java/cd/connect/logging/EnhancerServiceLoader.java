package cd.connect.logging;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class EnhancerServiceLoader {
  static List<JsonLogEnhancer> findJsonLogEnhancers() {
    List<JsonLogEnhancer> enhancers = new ArrayList<>();

    ServiceLoader<JsonLogEnhancer> serviceLoader = ServiceLoader.load(JsonLogEnhancer.class, Thread.currentThread().getContextClassLoader());
    Iterator<JsonLogEnhancer> iterator = serviceLoader.iterator();
    while (iterator.hasNext()) {
      enhancers.add(iterator.next());
    }
//    jsonLogEnhancers.forEach(enhancers::add);

    // in place sort by priority
    enhancers.sort(new Comparator<JsonLogEnhancer>() {
      @Override
      public int compare(JsonLogEnhancer o1, JsonLogEnhancer o2) {
        return Integer.compare(o1.getMapPriority(), o2.getMapPriority());
      }
    });

    return enhancers;
  }
}
