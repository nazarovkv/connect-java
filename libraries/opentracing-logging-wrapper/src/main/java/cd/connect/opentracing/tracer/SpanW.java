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
	Map<String, String> baggageItems = new HashMap<>();
	Map<String, String> spanValues = new HashMap<>();
	private String operationName;

	@Override
	public SpanContext context() {
		return new SpanContext() {
			@Override
			public Iterable<Map.Entry<String, String>> baggageItems() {
				return baggageItems.entrySet();
			}
		};
	}

	@Override
	public Span setTag(String key, String value) {
		spanValues.put(key, value);
		return this;
	}

	@Override
	public Span setTag(String key, boolean value) {
		spanValues.put(key, Boolean.toString(value));
		return null;
	}

	@Override
	public Span setTag(String key, Number value) {
		spanValues.put(key, value.toString());
		return null;
	}

	@Override
	public Span log(Map<String, ?> fields) {
		return this;
	}

	@Override
	public Span log(long timestampMicroseconds, Map<String, ?> fields) {
		return this;
	}

	@Override
	public Span log(String event) {
		return this;
	}

	@Override
	public Span log(long timestampMicroseconds, String event) {
		return this;
	}

	@Override
	public Span setBaggageItem(String key, String value) {
		baggageItems.put(key, value);
		return this;
	}

	@Override
	public String getBaggageItem(String key) {
		return baggageItems.get(key);
	}

	@Override
	public Span setOperationName(String operationName) {
		this.operationName = operationName;
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
