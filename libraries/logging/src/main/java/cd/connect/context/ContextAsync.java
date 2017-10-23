package cd.connect.context;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class ContextAsync {
  public static <T> Consumer<T> chainContext(final Consumer<T> original) {
    final Map<String, String> originalContext = ConnectContext.getContext();

    return new Consumer<T>() {
      @Override
      public void accept(T t) {
        try {
          ConnectContext.setContext(originalContext);

          original.accept(t);
        } finally {
          ConnectContext.clear();
        }
      }
    };
  }

  public static <T>Supplier<T> chainContext(final Supplier<T> original) {
    final Map<String, String> originalContext = ConnectContext.getContext();

    return new Supplier<T>() {
      @Override
      public T get() {
        try {
          ConnectContext.setContext(originalContext);
          return original.get();
        } finally {
          ConnectContext.clear();
        }
      }
    };
  }

  public static Runnable chainContext(final Runnable runnable) {
    final Map<String, String> originalContext = ConnectContext.getContext();

    return new Runnable() {
      @Override
      public void run() {
        try {
          ConnectContext.setContext(originalContext);
          runnable.run();
        } finally {
          ConnectContext.clear();
        }
      }
    };
  }
}
