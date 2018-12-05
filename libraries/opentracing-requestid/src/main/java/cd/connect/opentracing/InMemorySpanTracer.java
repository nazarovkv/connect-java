package cd.connect.opentracing;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class InMemorySpanTracer implements Tracer {
  private static final Logger log = LoggerFactory.getLogger(InMemorySpanTracer.class);
  protected ThreadLocal<Queue<InMemoryScope>> activeScopeStack = new ThreadLocal<>();

  void pushScope(InMemoryScope scope) {
    Queue<InMemoryScope> scopes = this.activeScopeStack.get();
    if (scopes == null) {
      scopes = new LinkedList<>();
      this.activeScopeStack.set(scopes);
    }
    scopes.add(scope);
  }

  InMemoryScope popScope() {
    Queue<InMemoryScope> scopes = this.activeScopeStack.get();
    if (scopes != null && scopes.size() > 0) {
      scopes.poll();
      return scopes.peek();
    } else {
      return null;
    }
  }

  InMemoryScope activeScope() {
    Queue<InMemoryScope> scopes = this.activeScopeStack.get();
    return (scopes != null && scopes.size() > 0) ? scopes.peek() : null;
  }

  boolean activeScopeClosed() {
    Queue<InMemoryScope> scopes = this.activeScopeStack.get();
    return (scopes != null && scopes.size() > 0) && scopes.peek().isClosed();
  }


  private final ScopeManager scopeManager = new ScopeManager() {
    @Override
    public Scope activate(Span span, boolean finishSpanOnClose) {
      InMemorySpan noSpan = (InMemorySpan) span;
      InMemoryScope scope = new InMemoryScope(noSpan, finishSpanOnClose, InMemorySpanTracer.this);
      pushScope(scope);

      return scope;
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
    InMemoryScope scope = cleanScopes();
    return (scope != null) ? scope.span() : null;
  }

  private InMemoryScope cleanScopes() {
    if (activeScopeClosed()) {
      popScope(); // remove the last one
      InMemoryScope scope = activeScope();

      while (scope != null && scope.isClosed()) {
        scope = popScope();
      }

      log.debug("scope count outstanding: {}", activeScopeStack.get().size());
      return scope;
    } else {
      log.debug("active scope still open");
      return activeScope();
    }
  }

  public void cleanupScope(InMemoryScope scope) {
    cleanScopes();
  }

  @Override
  public SpanBuilder buildSpan(String operationName) {
    return new InMemorySpanBuilder(operationName);
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

//    try {
//      throw new RuntimeException("re");
//    } catch (RuntimeException re) {
//      log.debug("extract called", re);
//    }

    if (format == Format.Builtin.HTTP_HEADERS) {
      newSpan = InMemorySpan.extractTextMap((TextMap) carrier);
    } else if (format == Format.Builtin.TEXT_MAP) {
      newSpan = InMemorySpan.extractTextMap((TextMap) carrier);
    } else if (format == Format.Builtin.BINARY) {
      log.error("No support for binary headers");
    } else {
      log.error("Unknown format");
    }

    return newSpan;
  }


  class InMemorySpanBuilder implements SpanBuilder {
    private InMemorySpan span;

    InMemorySpanBuilder(String opName) {
      span = new InMemorySpan(UUID.randomUUID().toString(), activeScope() == null ? null : activeScope().span);
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
      if (References.CHILD_OF.equals(referenceType)) {
        return asChildOf(referencedContext);
      } else { // follows from
        if (referencedContext instanceof InMemorySpan) {
          span.setPriorSpan((InMemorySpan)referencedContext);
        }
      }

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
