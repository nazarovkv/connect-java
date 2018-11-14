package cd.connect.opentracing;

import cd.connect.context.ConnectContext;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import java.util.UUID;

/**
 * This class is designed to ensure that the Connect context is able to extract
 * the identified context and write it to the logs.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class ConnectLoggingTracer implements Tracer {
	private final Tracer wrappedTracer;
	private final static String REQUEST_ID = "request-id";

	public ConnectLoggingTracer(Tracer wrappedTracer) {
		this.wrappedTracer = wrappedTracer;
	}


	@Override
	public ScopeManager scopeManager() {
		return wrappedTracer.scopeManager();
	}

	@Override
	public Span activeSpan() {
		return wrappedTracer.activeSpan();
	}

	class LoggingSpanBuilder implements  Tracer.SpanBuilder {
		private final SpanBuilder spanBuilder;

		LoggingSpanBuilder(SpanBuilder spanBuilder) {
			this.spanBuilder = spanBuilder;
		}

		@Override
		public SpanBuilder asChildOf(SpanContext parent) {
			return spanBuilder.asChildOf(parent);
		}

		@Override
		public SpanBuilder asChildOf(Span parent) {
			return spanBuilder.asChildOf(parent);
		}

		@Override
		public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
			return spanBuilder.addReference(referenceType, referencedContext);
		}

		@Override
		public SpanBuilder ignoreActiveSpan() {
			return spanBuilder.ignoreActiveSpan();
		}

		@Override
		public SpanBuilder withTag(String key, String value) {
			return spanBuilder.withTag(key, value);
		}

		@Override
		public SpanBuilder withTag(String key, boolean value) {
			return spanBuilder.withTag(key, value);
		}

		@Override
		public SpanBuilder withTag(String key, Number value) {
			return spanBuilder.withTag(key, value);
		}

		@Override
		public SpanBuilder withStartTimestamp(long microseconds) {
			return spanBuilder.withStartTimestamp(microseconds);
		}

		@Override
		public Scope startActive(boolean finishSpanOnClose) {
			Scope scope = spanBuilder.startActive(finishSpanOnClose);

			if (scope.span().getBaggageItem(REQUEST_ID) == null) {
				scope.span().setBaggageItem(REQUEST_ID, UUID.randomUUID().toString());
			}

			return scope;
		}

		@Override
		public Span startManual() {
			Span span = spanBuilder.startManual();

			if (span.getBaggageItem(REQUEST_ID) == null) {
				span.setBaggageItem(REQUEST_ID, UUID.randomUUID().toString());
			}

			return span;
		}

		@Override
		public Span start() {
			Span span = spanBuilder.start();

			if (span.getBaggageItem(REQUEST_ID) == null) {
				span.setBaggageItem(REQUEST_ID, UUID.randomUUID().toString());
			}

			return span;
		}
	}

	@Override
	public SpanBuilder buildSpan(String s) {
		return new LoggingSpanBuilder(wrappedTracer.buildSpan(s));
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C c) {
		wrappedTracer.inject(spanContext, format, c);
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C c) {
		SpanContext ctx = wrappedTracer.extract(format, c);
		
		ctx.baggageItems().forEach(entry -> {
			if (REQUEST_ID.equals(entry.getKey())) {
				ConnectContext.requestId.set(entry.getValue());
			}
		});
		return ctx;
	}
}
