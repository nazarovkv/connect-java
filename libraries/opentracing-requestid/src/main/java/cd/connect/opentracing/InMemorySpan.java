package cd.connect.opentracing;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.TextMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class InMemorySpan implements Span, SpanContext {
  private static final Logger log = LoggerFactory.getLogger(InMemorySpan.class);
  public static final String X_ACCEL_PREFIX = "x-acc-";
  public static final String X_ACCEL_TRACEID = X_ACCEL_PREFIX + "traceid";
  public static final String X_ACCEL_HEADERS = X_ACCEL_PREFIX + "baggage";
  private final String id;
  private final Consumer<InMemorySpan> cleanupCallback;
  private Map<String, String> baggage = new HashMap<>();
  private Map<String, String> tags = new HashMap<>();
  private AtomicInteger garbageCounter = new AtomicInteger(0);
  private InMemorySpan priorActiveSpan;
  private boolean priorSpanSetting = false;

  InMemorySpan(String id, InMemorySpan priorActiveSpan, Consumer<InMemorySpan> cleanupCallback) {
    log.debug("new span created with id {}", id);
    this.id = id;
    this.priorActiveSpan = priorActiveSpan;
    this.cleanupCallback = cleanupCallback;

    if (priorActiveSpan != null) {
      baggage.putAll(priorActiveSpan.baggage);
    }
  }

  public String getId() {
    return id;
  }

  public boolean isFinished() {
    return garbageCounter.get() == 0;
  }


  static InMemorySpan extractTextMap(TextMap map, Consumer<InMemorySpan> cleanupCallback) {
    log.debug("attempting to extract trace");
    // make a copy we can jump into
    Map<String, String> copy = new HashMap<>();
    map.iterator().forEachRemaining(c -> copy.put(c.getKey().toLowerCase(), c.getValue()));

    // are we part of an existing opentracing request? if no, start a new one.
    String id = copy.get(X_ACCEL_TRACEID);

    if (id != null) {
      InMemorySpan span = new InMemorySpan(id, null, cleanupCallback);

      // check for baggage
      String baggage = copy.get(X_ACCEL_HEADERS);
      if (baggage != null) {
        Arrays.stream(baggage.split(",")).forEach(b -> {
          String bItem = copy.get(X_ACCEL_PREFIX + b.toLowerCase());
          if (bItem != null) {
            span.setBaggageItem(b, bItem.replace("\\\\", "\\"));
          } else {
            log.error("Opentracing indicates propagated header {} but is missing", b);
          }
        });
      }

      log.debug("baggage is {}", span.baggage);

      return span;
    }

    log.debug("no trace found");

    return null;
  }

  @Override
  public SpanContext context() {
    return this;
  }

  public InMemorySpan getPriorSpan() {
    return priorActiveSpan;
  }

  public InMemorySpan setPriorSpan(InMemorySpan newPriorSpan) {
    if (priorSpanSetting) {
      this.priorActiveSpan = newPriorSpan;
    } else {
      priorSpanSetting = true; // detect loops
      if (priorActiveSpan != newPriorSpan && newPriorSpan != null) {
        baggage.putAll(newPriorSpan.baggage);
      }
      if (priorActiveSpan != null && priorActiveSpan != newPriorSpan && newPriorSpan != null) {
        // this could loop around and around, but the idea is to push the prior span further back
        newPriorSpan.setPriorSpan(priorActiveSpan);
      }
      this.priorActiveSpan = newPriorSpan;
      priorSpanSetting = false; // detect loops
    }

    return this;
  }

  @Override
  public Span setTag(String key, String value) {
    if (value == null) {
      tags.remove(key);
    } else {
      tags.put(key, value);
    }
    return this;
  }

  @Override
  public Span setTag(String key, boolean value) {
    setTag(key, Boolean.valueOf(value).toString());

    return this;
  }

  @Override
  public Span setTag(String key, Number value) {
    if (value == null) {
      tags.remove(key);
    } else {
      tags.put(key, value.toString());
    }

    return null;
  }

  @Override
  public Span log(Map<String, ?> fields) {
    log.debug("opentracing: {}", fields);
    return this;
  }

  @Override
  public Span log(long timestampMicroseconds, Map<String, ?> fields) {
    log.debug("opentracing: @{} -> {}", timestampMicroseconds, fields);
    return this;
  }

  @Override
  public Span log(String event) {
    log.debug("opentracing: event {}", event);
    return this;
  }

  // 0.3.0 standard?
  public Span log(String event, String message) {
    log.debug("opentracing: event {}, message {}", event, message);
    return this;
  }

  @Override
  public Span log(long timestampMicroseconds, String event) {
    log.debug("opentracing: event {} @{}", event, timestampMicroseconds);
    return this;
  }

  @Override
  public Span setBaggageItem(String key, String value) {
    log.debug("setting baggage: {} = `{}`", key, value);
    baggage.put(key, value);
    return this;
  }

  @Override
  public String getBaggageItem(String key) {
    return baggage.get(key);
  }

  @Override
  public Span setOperationName(String operationName) {
    return this;
  }

  @Override
  public void finish() {
    if (garbageCounter.decrementAndGet() == 0) {
      cleanupCallback.accept(this);
    } else {
      try {
        throw new RuntimeException("here");
      } catch (RuntimeException re) {

        log.debug("inmem ignoring finish - {}: {}", garbageCounter.get(), id, re);
      }
    }
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
      log.debug("inmem new interest {}: {}", garbageCounter.get(), id, re);
    }
  }

  @Override
  public Iterable<Map.Entry<String, String>> baggageItems() {
    return baggage.entrySet();
  }

  public void injectTextMap(TextMap map) {
    map.put(X_ACCEL_TRACEID, id);
    log.debug("inject baggage: {}", baggage);
    if (baggage.size() > 0) {
      map.put(X_ACCEL_HEADERS, String.join(",", baggage.keySet()));

      baggage.forEach((k, v) -> {
        map.put(X_ACCEL_PREFIX + k, v.replace("\\", "\\\\"));
      });
    }
  }
}
