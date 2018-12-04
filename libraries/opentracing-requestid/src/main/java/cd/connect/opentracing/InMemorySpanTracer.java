package cd.connect.opentracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class InMemorySpanTracer implements Tracer, Consumer<InMemorySpan> {
  private static final Logger log = LoggerFactory.getLogger(InMemorySpanTracer.class);
  protected ThreadLocal<InMemoryScope> spans = new ThreadLocal<>();

  private final ScopeManager scopeManager = new ScopeManager() {
    @Override
    public Scope activate(Span span, boolean finishSpanOnClose) {
      InMemorySpan noSpan = (InMemorySpan) span;
      spans.set(new InMemoryScope(noSpan));

      return spans.get();
    }

    @Override
    public Scope active() {
      return spans.get();
    }
  };



  @Override
  public ScopeManager scopeManager() {
    return scopeManager;
  }

  @Override
  public Span activeSpan() {
    InMemoryScope scope = spans.get();
    return scope == null ? null : scope.span;
  }

  @Override
  public void accept(InMemorySpan finishingSpan) {
    InMemorySpan priorSpan = finishingSpan.getPriorSpan();

    log.debug("span {} has been finished: is it active? {}", finishingSpan.getId(), activeSpan() == finishingSpan);
    if (activeSpan() == finishingSpan) {
      spans.remove();

      while (priorSpan != null && priorSpan.isFinished()) {
        priorSpan = priorSpan.getPriorSpan();
      }

      if (priorSpan != null) {
        spans.set(new InMemoryScope(priorSpan));
      }
    }
  }

  @Override
  public SpanBuilder buildSpan(String operationName) {
    return new InMemorySpanBuilder(operationName, this);
  }

  @Override
  public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
    if (spanContext instanceof InMemorySpan) {
      InMemorySpan span = (InMemorySpan) spanContext;

      if (format == Format.Builtin.HTTP_HEADERS) {
        span.injectTextMap((TextMap) carrier);
      } else if (format == Format.Builtin.TEXT_MAP) {
        span.injectTextMap((TextMap) carrier);
      } else if (format == Format.Builtin.BINARY) {
        log.error("No support for binary headers");
      }
    }
  }

  @Override
  public <C> SpanContext extract(Format<C> format, C carrier) {
    InMemorySpan newSpan = null;

    if (format == Format.Builtin.HTTP_HEADERS) {
      newSpan = InMemorySpan.extractTextMap((TextMap) carrier,  this);
    } else if (format == Format.Builtin.TEXT_MAP) {
      newSpan = InMemorySpan.extractTextMap((TextMap) carrier, this);
    } else if (format == Format.Builtin.BINARY) {
      log.error("No support for binary headers");
    } else {
      log.error("Unknown format");
    }

    return newSpan;
  }

  // when one of these gets created, another client is interested in this span
  // so we increase its garbage count. This is how Executor pools propagate spans
  // by creating new scopes foe them after getting the active span.
  class InMemoryScope implements Scope {
    private final InMemorySpan span;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    InMemoryScope(InMemorySpan span) {
      this.span = span;
      span.incInterest();
    }

    @Override
    public void close() {
      if (closed.compareAndSet(false, true) && span != null) {
        span.finish();
      }
    }

    @Override
    public Span span() {
      return span;
    }
  }

  class InMemorySpanBuilder implements SpanBuilder {
    private InMemorySpan span;

    InMemorySpanBuilder(String opName, Consumer<InMemorySpan> callback) {
      span = new InMemorySpan(UUID.randomUUID().toString(), spans.get() == null ? null : spans.get().span, callback);
      span.setOperationName(opName);
    }

    @Override
    public SpanBuilder asChildOf(SpanContext parent) {
      log.debug("setting {} as parent span of {}", ((InMemorySpan)parent).getId(), span.getId());
      span.setPriorSpan((InMemorySpan)parent);
      return this;
    }

    @Override
    public SpanBuilder asChildOf(Span parent) {
      log.debug("setting {} as parent span of {}", ((InMemorySpan)parent).getId(), span.getId());
      span.setPriorSpan((InMemorySpan)parent);
      return this;
    }

    @Override
    public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
      return this;
    }

    @Override
    public SpanBuilder ignoreActiveSpan() {
      span.setPriorSpan(null);
      return this;
    }

    @Override
    public SpanBuilder withTag(String key, String value) {
      span.setTag(key, value);
      return this;
    }

    @Override
    public SpanBuilder withTag(String key, boolean value) {
      span.setTag(key, value);
      return this;
    }

    @Override
    public SpanBuilder withTag(String key, Number value) {
      span.setTag(key, value);
      return this;
    }

    @Override
    public SpanBuilder withStartTimestamp(long microseconds) {
      return this;
    }

    @Override
    public Scope startActive(boolean finishSpanOnClose) {
      return scopeManager().activate(span, finishSpanOnClose);
    }

    @Override
    public Span startManual() {
      return startActive(true).span();
    }

    /**
     * this one isn't activated
     * @return
     */
    @Override
    public Span start() {
      return span;
    }
  }
}
