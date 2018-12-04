package cd.connect.opentracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is designed to ensure that the Connect context is able to extract
 * the identified context and write it to the logs.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class LoggingSpanTracer implements Tracer {
  private static final Logger log = LoggerFactory.getLogger(LoggingSpanTracer.class);
  private final Tracer wrappedTracer;
  private ThreadLocal<LoggerScope> spans = new ThreadLocal<>();

  public LoggingSpanTracer(Tracer wrappedTracer) {
    this.wrappedTracer = wrappedTracer;
  }

  void cleanup(LoggerSpan finishingSpan) {
    LoggerSpan priorSpan = finishingSpan.getPriorSpan();

    finishingSpan.removeActive();

    log.debug("loggerspan {} has been finished: is it active? {}", finishingSpan.getId(), activeSpan() == finishingSpan);
    if (activeSpan() == finishingSpan) {
      spans.remove();

      // if the span represented by this scope is finished, we want to walk further
      // back
      while (priorSpan != null && priorSpan.isFinished()) {
        priorSpan = priorSpan.getPriorSpan();
      }

//      if (priorSpan != null) {
//        spans.set(new LoggerScope(priorSpan, true));
//      }
    }
  }

  private final ScopeManager scopeManager = new ScopeManager() {
    @Override
    public Scope activate(Span span, boolean finishSpanOnClose) {
      LoggerSpan loggerSpan = (LoggerSpan) span;

      LoggerScope newScope = new LoggerScope(loggerSpan, finishSpanOnClose);

      Scope wrappedScope = wrappedTracer.scopeManager().activate(loggerSpan.getWrappedSpan(), finishSpanOnClose);
      newScope.setWrappedScope(wrappedScope);

      loggerSpan.setActive();

      if (loggerSpan.getBaggageItem(OpenTracingLogger.WELL_KNOWN_REQUEST_ID) == null) {
        loggerSpan.setBaggageItem(OpenTracingLogger.WELL_KNOWN_REQUEST_ID, OpenTracingLogger.randomRequestIdProvider.get());
      }

      spans.set(newScope);

      return newScope;
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
    LoggerScope scope = spans.get();
    return scope == null ? null : scope.span();
  }

  @Override
  public SpanBuilder buildSpan(String operationName) {
    return new LoggingSpanBuilder(wrappedTracer.buildSpan(operationName));
  }

  @Override
  public <C> void inject(SpanContext spanContext, Format<C> format, C c) {
    // get the internal span and inject its data, we have none
    LoggerSpan span = (LoggerSpan)spanContext;
    wrappedTracer.inject(span.getWrappedSpan().context(), format, c);
  }

  @Override
  public <C> SpanContext extract(Format<C> format, C c) {
    SpanContext ctx = wrappedTracer.extract(format, c);

    LoggerSpan span = null;
    if (ctx != null) {

      span = new LoggerSpan(this, null);

      if (ctx instanceof Span) {
        span.setWrappedSpan((Span)ctx);
      }

      LoggerSpan finalSpan = span;
      ctx.baggageItems().forEach(entry -> {
        finalSpan.setBaggageItem(entry.getKey(), entry.getValue());
      });
    }

    return span;
  }

  class LoggingSpanBuilder implements Tracer.SpanBuilder {
    private SpanBuilder spanBuilder;
    private LoggerSpan loggerSpan;

    LoggingSpanBuilder(SpanBuilder spanBuilder) {
      this.spanBuilder = spanBuilder;
      loggerSpan = new LoggerSpan(LoggingSpanTracer.this, spans.get());
    }

    @Override
    public SpanBuilder asChildOf(SpanContext parent) {
      if (parent instanceof LoggerSpan) {
        this.spanBuilder = this.spanBuilder.asChildOf(((LoggerSpan)parent).getWrappedSpan());
        loggerSpan.setPriorSpan((LoggerSpan)parent);
      } else {
        this.spanBuilder = this.spanBuilder.asChildOf(parent);
      }

      return this;
    }

    @Override
    public SpanBuilder asChildOf(Span parent) {
      if (parent instanceof LoggerSpan) {
        this.spanBuilder = this.spanBuilder.asChildOf(((LoggerSpan)parent).getWrappedSpan());
        loggerSpan.setPriorSpan((LoggerSpan)parent);
      } else {
        this.spanBuilder = this.spanBuilder.asChildOf(parent);
      }

      return this;
    }

    @Override
    public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
      this.spanBuilder = spanBuilder.addReference(referenceType, referencedContext);
      // we don't use it, so ignore it
      return this;
    }

    @Override
    public SpanBuilder ignoreActiveSpan() {
      this.spanBuilder = spanBuilder.ignoreActiveSpan();
      loggerSpan.setPriorSpan(null);
      return this;
    }

    @Override
    public SpanBuilder withTag(String key, String value) {
      loggerSpan.setTag(key, value);
      spanBuilder = spanBuilder.withTag(key, value);
      return this;
    }

    @Override
    public SpanBuilder withTag(String key, boolean value) {
      loggerSpan.setTag(key, value);
      spanBuilder = spanBuilder.withTag(key, value);
      return this;
    }

    @Override
    public SpanBuilder withTag(String key, Number value) {
      loggerSpan.setTag(key, value);
      spanBuilder = spanBuilder.withTag(key, value);
      return this;
    }

    @Override
    public SpanBuilder withStartTimestamp(long microseconds) {
      spanBuilder = spanBuilder.withStartTimestamp(microseconds);
      return this;
    }

    @Override
    public Scope startActive(boolean finishSpanOnClose) {
      Scope scope = spanBuilder.startActive(finishSpanOnClose);

      // we can't use "activate" in our own scopeManager as it will activate the wrapped span again
      loggerSpan.setWrappedSpan(scope.span());

      LoggerScope newScope = new LoggerScope(loggerSpan, finishSpanOnClose);

      newScope.setWrappedScope(scope);

      loggerSpan.setActive();

      if (loggerSpan.getBaggageItem(OpenTracingLogger.WELL_KNOWN_REQUEST_ID) == null) {
        loggerSpan.setBaggageItem(OpenTracingLogger.WELL_KNOWN_REQUEST_ID, OpenTracingLogger.randomRequestIdProvider.get());
      }

      spans.set(newScope);

      return newScope;
    }

    @Override
    public Span startManual() {
      return startActive(false).span();
    }

    @Override
    public Span start() {
      return startActive(true).span();
    }
  }
}
