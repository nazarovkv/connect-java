package cd.connect.opentracing.tracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class SpanW implements Span {
  private final Span wrappedSpan;

  public SpanW(Span wrappedSpan) {
    this.wrappedSpan = wrappedSpan;
  }

  @Override
  public SpanContext context() {
    return wrappedSpan.context();
  }

  @Override
  public Span setTag(String key, String value) {
    wrappedSpan.setTag(key, value);
    return this;
  }

  @Override
  public Span setTag(String key, boolean value) {
    wrappedSpan.setTag(key, value);
    return this;
  }

  @Override
  public Span setTag(String key, Number value) {
    wrappedSpan.setTag(key, value);
    return this;
  }

  @Override
  public Span log(Map<String, ?> fields) {
    wrappedSpan.log(fields);
    return this;
  }

  @Override
  public Span log(long timestampMicroseconds, Map<String, ?> fields) {
    wrappedSpan.log(timestampMicroseconds, fields);
    return this;
  }

  @Override
  public Span log(String event) {
    wrappedSpan.log(event);
    return this;
  }

  @Override
  public Span log(long timestampMicroseconds, String event) {
    wrappedSpan.log(timestampMicroseconds, event);
    return this;
  }

  @Override
  public Span setBaggageItem(String key, String value) {
    wrappedSpan.setBaggageItem(key, value);
    return this;
  }

  @Override
  public String getBaggageItem(String key) {
    return wrappedSpan.getBaggageItem(key);
  }

  @Override
  public Span setOperationName(String operationName) {
    wrappedSpan.setOperationName(operationName);
    return this;
  }

  @Override
  public void finish() {
    MDC.clear();
  }

  @Override
  public void finish(long finishMicros) {
    MDC.clear();
  }
}
