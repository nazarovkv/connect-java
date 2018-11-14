package cd.connect.opentracing;

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.AutoFinishScopeManager;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class ConnectTracer implements Tracer {
	AutoFinishScopeManager scopeManager = new AutoFinishScopeManager();

	@Override
	public ScopeManager scopeManager() {
		return scopeManager;
	}

	@Override
	public Span activeSpan() {
		return scopeManager.active().span();
	}

	@Override
	public SpanBuilder buildSpan(String s) {
		return null;
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C c) {

	}

	@Override
	public <C> SpanContext extract(Format<C> format, C c) {
		return null;
	}
}
