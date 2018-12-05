package cd.connect.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *   // when one of these gets created, another client is interested in this span
 *   // so we increase its garbage count. This is how Executor pools propagate spans
 *   // by creating new scopes foe them after getting the active span.
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class InMemoryScope implements Scope {
  private static final Logger log = LoggerFactory.getLogger(InMemoryScope.class);
  final InMemorySpan span;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final boolean finishOnClosed;
  private final InMemorySpanTracer tracer;

  InMemoryScope(InMemorySpan span, boolean finishOnClosed, InMemorySpanTracer tracer) {
    this.span = span;
    this.finishOnClosed = finishOnClosed;
    this.tracer = tracer;

    log.debug("inmem activating new scope with span {}", span.getId());

    if (finishOnClosed) {
      span.incInterest();
    }
  }

  boolean isClosed() {
    return closed.get();
  }

  @Override
  public void close() {
    log.debug("inmem closing scope {}", span.getId());
    if (closed.compareAndSet(false, true)) {
      if (span != null && finishOnClosed) {
        span.finish();
      }

      tracer.cleanupScope(this);
    }
  }

  @Override
  public Span span() {
    return span;
  }
}
