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
import java.util.function.Consumer;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class NoSpanTracer implements Tracer, Consumer<NoSpanSpan> {
  private static final Logger log = LoggerFactory.getLogger(NoSpanTracer.class);
  private ThreadLocal<NoSpanSpan> spans = new ThreadLocal<>();

  @Override
  public ScopeManager scopeManager() {
    return new ScopeManager() {
      @Override
      public Scope activate(Span span, boolean finishSpanOnClose) {
        NoSpanSpan prior = spans.get();
        NoSpanSpan newActiveSpan = (NoSpanSpan) span;
        if (prior != null) {
          (newActiveSpan).setPriorSpan(prior);
        }

        spans.set(newActiveSpan);
        return new NoSpanScope(newActiveSpan);
      }

      @Override
      public Scope active() {
        NoSpanSpan active = spans.get();

        if (active != null) {
          return new NoSpanScope(active);
        }

        return null;
      }
    };
  }

  @Override
  public Span activeSpan() {
    return spans.get();
  }

  protected NoSpanSpan ensureWeHaveActiveSpan() {
    NoSpanSpan noSpanSpan = spans.get();
    if (noSpanSpan == null) {
      noSpanSpan = new NoSpanSpan(UUID.randomUUID().toString(), null, this);
      spans.set(noSpanSpan);
    }

    return noSpanSpan;
  }

  @Override
  public void accept(NoSpanSpan finishingSpan) {
    NoSpanSpan priorSpan = finishingSpan.getPriorSpan();

    log.info("required to remove span: {}", spans.get() == finishingSpan);
    if (spans.get() == finishingSpan) {
      spans.remove();

      if (priorSpan != null) {
        spans.set(priorSpan);
      }
    }
  }

  @Override
  public SpanBuilder buildSpan(String operationName) {
    return new NoSpanSpanBuilder();
  }

  @Override
  public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
    if (spanContext instanceof NoSpanSpan) {
      NoSpanSpan span = (NoSpanSpan) spanContext;

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
    NoSpanSpan newSpan = null;

    if (format == Format.Builtin.HTTP_HEADERS) {
      newSpan = NoSpanSpan.extractTextMap((TextMap) carrier, spans.get(), this);
    } else if (format == Format.Builtin.TEXT_MAP) {
      newSpan = NoSpanSpan.extractTextMap((TextMap) carrier, spans.get(), this);
    } else if (format == Format.Builtin.BINARY) {
      log.error("No support for binary headers");
    } else {
      log.error("Unknown format");
    }

    if (newSpan != null) {
      spans.set(newSpan);
    }

    return newSpan;
  }

  class NoSpanScope implements Scope {
    private final NoSpanSpan span;

    NoSpanScope(NoSpanSpan span) {
      this.span = span;
      span.incInterest();
    }

    @Override
    public void close() {
      span.finish();
    }

    @Override
    public Span span() {
      return span;
    }
  }

  class NoSpanSpanBuilder implements SpanBuilder {

    @Override
    public SpanBuilder asChildOf(SpanContext parent) {
      return this;
    }

    @Override
    public SpanBuilder asChildOf(Span parent) {
      return this;
    }

    @Override
    public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
      return this;
    }

    @Override
    public SpanBuilder ignoreActiveSpan() {
      return this;
    }

    @Override
    public SpanBuilder withTag(String key, String value) {
      return this;
    }

    @Override
    public SpanBuilder withTag(String key, boolean value) {
      return this;
    }

    @Override
    public SpanBuilder withTag(String key, Number value) {
      return this;
    }

    @Override
    public SpanBuilder withStartTimestamp(long microseconds) {
      return this;
    }

    @Override
    public Scope startActive(boolean finishSpanOnClose) {
      return new NoSpanScope(ensureWeHaveActiveSpan());
    }

    @Override
    public Span startManual() {
      NoSpanSpan span = ensureWeHaveActiveSpan();
      span.incInterest();
      return span;
    }

    @Override
    public Span start() {
      return startManual();
    }
  }
}
