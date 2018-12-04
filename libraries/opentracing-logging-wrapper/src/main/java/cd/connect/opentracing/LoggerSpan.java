package cd.connect.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is designed to extract the baggage items of the active trace
 * into the logging context, and on activation, the tags go into the logs as well
 * 
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class LoggerSpan implements Span, SpanContext {
  private static final Logger log = LoggerFactory.getLogger(LoggerSpan.class);
  private Span wrappedSpan;
  private Map<String, String> baggage = new HashMap<>();
  private Map<String, Object> tags = new HashMap<>();
  private Map<String, Object> logs = new HashMap<>();
  private List<String> events = new ArrayList<>();
  private final LoggingSpanTracer tracer;
  private LoggerScope priorActiveScope;
  private boolean priorSpanSetting = false;
  private final String id;
  private AtomicInteger garbageCounter = new AtomicInteger(0);


  public LoggerSpan(LoggingSpanTracer tracer, LoggerScope priorActiveSpan) {
    this.tracer = tracer;
    this.id = UUID.randomUUID().toString();
    if (priorActiveSpan != null) {
      baggage.putAll(((LoggerSpan)priorActiveSpan.span()).baggage);
    }
  }

  public boolean isFinished() {
    return garbageCounter.get() == 0;
  }

  // if we are doing an extract from a header, the returned spancontext may
  // not be a span, so we may need to set it later once the span builder has
  // created it.
  public void setWrappedSpan(Span wrappedSpan) {
    this.wrappedSpan = wrappedSpan;
  }

  @Override
  public SpanContext context() {
    return this;
  }

  LoggerScope getPriorScope() {
    return priorActiveScope;
  }

  void setPriorScope(LoggerScope newPriorScope) {
    LoggerSpan priorSpan = (newPriorScope == null) ? null : ((LoggerSpan)newPriorScope.span());

    if (priorSpanSetting) {
      this.priorActiveScope = newPriorScope;
    } else {
      priorSpanSetting = true; // detect loops
      if (priorActiveScope != newPriorScope && newPriorScope != null) {
        baggage.putAll(priorSpan.baggage);
      }
      if (priorActiveScope != null && priorActiveScope != newPriorScope && newPriorScope != null) {
        // this could loop around and around, but the idea is to push the prior span further back
        priorSpan.setPriorScope(priorActiveScope);
      }
      this.priorActiveScope = newPriorScope;
      priorSpanSetting = false; // detect loops
    }
  }

  @Override
  public Span setTag(String key, String value) {
    if (value == null) {
      tags.remove(key);
    } else {
      tags.put(key, value);
    }
    if (wrappedSpan != null) {
      wrappedSpan.setTag(key, value);
    }
    updateLoggedTags();
    return this;
  }

  @Override
  public Span setTag(String key, boolean value) {
    tags.put(key, value);
    if (wrappedSpan != null) {
      wrappedSpan.setTag(key, value);
    }
    updateLoggedTags();
    return this;
  }

  @Override
  public Span setTag(String key, Number value) {
    if (value == null) {
      tags.remove(key);
    } else {
      tags.put(key, value);
    }
    if (wrappedSpan != null) {
      wrappedSpan.setTag(key, value);
    }
    updateLoggedTags();
    return null;
  }

  @Override
  public Span log(Map<String, ?> fields) {
    fields.forEach((key, value) -> logs.put(key, value));
    wrappedSpan.log(fields);
    updateLoggedMessages();
    return this;
  }

  @Override
  public Span log(long timestampMicroseconds, Map<String, ?> fields) {
    log(fields);
    wrappedSpan.log(timestampMicroseconds, fields);
    updateLoggedMessages();
    return this;
  }

  @Override
  public Span log(String event) {
    this.events.add(event);
    wrappedSpan.log(event);
    return this;
  }

  // 0.3.0 standard, prevents older library usage from barfing
  public Span log(String event, String message) {
    this.events.add(event + ":" + message);
    wrappedSpan.log(event);
    return this;
  }

  @Override
  public Span log(long timestampMicroseconds, String event) {
    this.events.add(event + "@" + timestampMicroseconds);
    wrappedSpan.log(timestampMicroseconds, event);
    return this;
  }

  @Override
  public Span setBaggageItem(String key, String value) {
    // we keep it as well because we cannot ensure the wrapped span is also a spancontext
    baggage.put(key, value);
    wrappedSpan.setBaggageItem(key, value);
    updateLoggedBaggage();
    return this;
  }

  // there is no way to set baggage on an item until after you have started it and made it active, so a typical
  // trace will not log its own baggage
  private void updateLoggedBaggage() {
    if (id.equals(MDC.get(OPENTRACING_ID))) {
      MDC.put(OPENTRACING_BAGGAGE, ObjectMapperProvider.wrapObject(baggage));
    }
  }

  private void updateLoggedMessages() {
    if (id.equals(MDC.get(OPENTRACING_ID))) {
      MDC.put(OPENTRACING_LOG_MESSAGES, ObjectMapperProvider.wrapObject(logs));
    }
  }

  private void updateLoggedTags() {
    if (id.equals(MDC.get(OPENTRACING_ID))) {
      MDC.put(OPENTRACING_TAGS, ObjectMapperProvider.wrapObject(tags));
    }
  }

  static final String OPENTRACING_BAGGAGE = "opentracing.baggage";
  static final String OPENTRACING_LOG_MESSAGES = "opentracing.logs";
  static final String OPENTRACING_TAGS = "opentracing.tags";
  static final String OPENTRACING_ID = "opentracing.id";


  void setActive() {
    MDC.put(OPENTRACING_ID, this.id);
    updateLoggedBaggage();
    updateLoggedMessages();
    updateLoggedTags();
  }

  void removeActive() {
    // don't remove if not ours
    if (id.equals(MDC.get(OPENTRACING_ID))) {
      log.info("removing tracing");
      MDC.remove(OPENTRACING_ID);
      MDC.remove(OPENTRACING_TAGS);
      MDC.remove(OPENTRACING_BAGGAGE);
      MDC.remove(OPENTRACING_LOG_MESSAGES);
      log.info("removed tracing");
    }
  }

  String getId() {
    return id;
  }

  Span getWrappedSpan() {
    return wrappedSpan;
  }

  @Override
  public String getBaggageItem(String key) {
    return wrappedSpan.getBaggageItem(key);
  }

  @Override
  public Span setOperationName(String operationName) {
    return this;
  }

  @Override
  public void finish() {
    if (garbageCounter.decrementAndGet() == 0) {
      tracer.cleanup(this);
    } else {
      try {
        throw new RuntimeException("here");
      } catch (RuntimeException re) {
        log.debug("logger ignoring finish - {}: {}", garbageCounter.get(), id, re);
      }
    }
    wrappedSpan.finish();
  }

  @Override
  public void finish(long finishMicros) {
    finish();
  }

  protected void incInterest() {
    garbageCounter.incrementAndGet();
    try {
      throw new RuntimeException("here");
    } catch (RuntimeException re) {
      log.debug("logger new interest {}: {}", garbageCounter.get(), id, re);
    }

  }

  @Override
  public Iterable<Map.Entry<String, String>> baggageItems() {
    return baggage.entrySet();
  }
}
