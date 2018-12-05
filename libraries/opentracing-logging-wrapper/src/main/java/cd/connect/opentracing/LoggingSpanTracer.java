package cd.connect.opentracing;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class is designed to ensure that the Connect context is able to extract
 * the identified context and write it to the logs.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class LoggingSpanTracer implements Tracer {
  private static final Logger log = LoggerFactory.getLogger(LoggingSpanTracer.class);
  private final Tracer wrappedTracer;
  protected ThreadLocal<Queue<LoggerScope>> activeScopeStack = new ThreadLocal<>();
  private String appName;


  void pushScope(LoggerScope scope) {
    Queue<LoggerScope> scopes = this.activeScopeStack.get();
    if (scopes == null) {
      scopes = new LinkedList<>();
      this.activeScopeStack.set(scopes);
    }
    scopes.add(scope);
  }

  LoggerScope popScope() {
    Queue<LoggerScope> scopes = this.activeScopeStack.get();
    if (scopes != null && scopes.size() > 0) {
      LoggerScope poll = scopes.poll();
      log.debug("dropping scope for span {}", poll.span.getId());
      return scopes.peek();
    } else {
      return null;
    }
  }

  LoggerScope activeScope() {
    Queue<LoggerScope> scopes = this.activeScopeStack.get();
    return (scopes != null && scopes.size() > 0) ? scopes.peek() : null;
  }

  boolean activeScopeClosed() {
    Queue<LoggerScope> scopes = this.activeScopeStack.get();
    return (scopes != null && scopes.size() > 0) && scopes.peek().isClosed();
  }

  public LoggingSpanTracer(Tracer wrappedTracer) {
    this.wrappedTracer = wrappedTracer;

    appName = System.getProperty("app.name");
    
    if (appName == null) {
      try {
        appName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        appName = "unknown-app";
      }
    }
  }

  private LoggerScope cleanScopes() {
    if (activeScopeClosed()) {
      popScope();
      LoggerScope scope = activeScope();
      while (scope != null && scope.isClosed()) {
        scope = popScope();
      }
      // take the last active scope and make its span active again for logging
      if (scope != null && !scope.span.isFinished()) {
        scope.span.setActive(appName);
      }

      log.debug("scope count outstanding: {}", activeScopeStack.get().size());
      return scope;
    } else {
      return activeScope();
    }
  }

  void cleanupScope(LoggerScope scope) {
    scope.span.removeActive();

    log.debug("loggerscope with span {} has been finished", scope.span.getId());
    cleanScopes();
  }

  private final ScopeManager scopeManager = new ScopeManager() {
    @Override
    public Scope activate(Span span, boolean finishSpanOnClose) {
      LoggerSpan loggerSpan = (LoggerSpan) span;

      LoggerScope newScope = new LoggerScope(loggerSpan, finishSpanOnClose, LoggingSpanTracer.this);

      Scope wrappedScope = wrappedTracer.scopeManager().activate(loggerSpan.getWrappedSpan(), finishSpanOnClose);
      newScope.setWrappedScope(wrappedScope);

      loggerSpan.setActive(appName);

      if (loggerSpan.getBaggageItem(OpenTracingLogger.WELL_KNOWN_REQUEST_ID) == null) {
        loggerSpan.setBaggageItem(OpenTracingLogger.WELL_KNOWN_REQUEST_ID, OpenTracingLogger.randomRequestIdProvider.get());
      }

      if (loggerSpan.getBaggageItem(OpenTracingLogger.WELL_KNOWN_ORIGIN_APP) == null) {
        loggerSpan.setBaggageItem(OpenTracingLogger.WELL_KNOWN_ORIGIN_APP, appName);
      }

      pushScope(newScope);

      return newScope;
    }

    @Override
    public Scope active() {
      return activeScope();
    }
  };

  @Override
  public ScopeManager scopeManager() {
    return scopeManager;
  }

  @Override
  public Span activeSpan() {
    LoggerScope scope = cleanScopes();
    return (scope == null) ? null : scope.span();
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

      span = new LoggerSpan(null);

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
      loggerSpan = new LoggerSpan(activeScope());
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
      if (References.CHILD_OF.equals(referenceType)) {
        return asChildOf(referencedContext);
      } else { // follows from
        if (referencedContext instanceof LoggerSpan) {
          this.spanBuilder = spanBuilder.addReference(referenceType, ((LoggerSpan)referencedContext).getWrappedSpan().context());
          loggerSpan.setPriorSpan((LoggerSpan)referencedContext);
        } else {
          this.spanBuilder = spanBuilder.addReference(referenceType, referencedContext);
        }
      }

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

      LoggerScope newScope = new LoggerScope(loggerSpan, finishSpanOnClose, LoggingSpanTracer.this);

      newScope.setWrappedScope(scope);

      loggerSpan.setActive(appName);

      if (loggerSpan.getBaggageItem(OpenTracingLogger.WELL_KNOWN_REQUEST_ID) == null) {
        loggerSpan.setBaggageItem(OpenTracingLogger.WELL_KNOWN_REQUEST_ID, OpenTracingLogger.randomRequestIdProvider.get());
      }

      pushScope(newScope);

      return newScope;
    }

    @Override
    public Span startManual() {
      return startActive(false).span();
    }

    @Override
    public Span start() {
      loggerSpan.setWrappedSpan(spanBuilder.start());
      return loggerSpan;
    }
  }
}
