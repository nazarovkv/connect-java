package cd.connect.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   // when one of these gets created, another client is interested in this span
 *   // so we increase its garbage count. This is how Executor pools propagate spans
 *   // by creating new scopes foe them after getting the active span.
 *   
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class LoggerScope implements Scope {
  final LoggerSpan span;
  private Scope wrappedScope;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final boolean finishSpanOnClose;

  LoggerScope(LoggerSpan span, boolean finishSpanOnClose) {
    this.span = span;
    this.finishSpanOnClose = finishSpanOnClose;
    if (finishSpanOnClose) {
      span.incInterest();
    }
  }

  public boolean isClosed() {
    return closed.get();
  }
  
  public void setWrappedScope(Scope wrappedScope) {
    this.wrappedScope = wrappedScope;
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      if (span != null && finishSpanOnClose) {
        span.finish(false); // don't call finish because wrappedScope.close will
      }
    }

    if (wrappedScope != null) {
      wrappedScope.close();
    }
  }

  @Override
  public Span span() {
    return span;
  }
}
