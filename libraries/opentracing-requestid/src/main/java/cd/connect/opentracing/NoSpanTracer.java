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
  protected ThreadLocal<NoSpanScope> spans = new ThreadLocal<>();

  @Override
  public ScopeManager scopeManager() {
    return new ScopeManager() {
      @Override
      public Scope activate(Span span, boolean finishSpanOnClose) {
        NoSpanSpan noSpan = (NoSpanSpan) span;
        spans.set(new NoSpanScope(noSpan));

        noSpan.incInterest();

        return spans.get();
      }

      @Override
      public Scope active() {
        return spans.get();
      }
    };
  }

  @Override
  public Span activeSpan() {
    NoSpanScope scope = spans.get();
    return scope == null ? null : scope.span;
  }

  @Override
  public void accept(NoSpanSpan finishingSpan) {
    NoSpanSpan priorSpan = finishingSpan.getPriorSpan();

    log.info("span {} has been finished: is it active? {}", finishingSpan.getId(), activeSpan() == finishingSpan);
    if (activeSpan() == finishingSpan) {
      spans.remove();

      if (priorSpan != null) {
        spans.set(new NoSpanScope(priorSpan));
      }
    }
  }

  @Override
  public SpanBuilder buildSpan(String operationName) {
    return new NoSpanSpanBuilder(operationName, this);
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
      newSpan = NoSpanSpan.extractTextMap((TextMap) carrier,  this);
    } else if (format == Format.Builtin.TEXT_MAP) {
      newSpan = NoSpanSpan.extractTextMap((TextMap) carrier, this);
    } else if (format == Format.Builtin.BINARY) {
      log.error("No support for binary headers");
    } else {
      log.error("Unknown format");
    }

    return newSpan;
  }

  class NoSpanScope implements Scope {
    private final NoSpanSpan span;

    NoSpanScope(NoSpanSpan span) {
      this.span = span;
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
    private NoSpanSpan span;

    NoSpanSpanBuilder(String opName, Consumer<NoSpanSpan> callback) {
      span = new NoSpanSpan(UUID.randomUUID().toString(), spans.get() == null ? null : spans.get().span, callback);
      span.setOperationName(opName);
    }

    @Override
    public SpanBuilder asChildOf(SpanContext parent) {
      log.info("setting {} as parent span of {}", ((NoSpanSpan)parent).getId(), span.getId());
      span.setPriorSpan((NoSpanSpan)parent);
      return this;
    }

    @Override
    public SpanBuilder asChildOf(Span parent) {
      log.info("setting {} as parent span of {}", ((NoSpanSpan)parent).getId(), span.getId());
      span.setPriorSpan((NoSpanSpan)parent);
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
      startActive(true);
      return span;
    }

    /**
     * this one isn't activated
     * @return
     */
    @Override
    public Span start() {
      span.incInterest();
      return span;
    }
  }
}
