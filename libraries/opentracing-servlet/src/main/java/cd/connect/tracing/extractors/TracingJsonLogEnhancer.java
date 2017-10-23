package cd.connect.tracing.extractors;

import cd.connect.logging.JsonLogEnhancer;

import java.util.List;
import java.util.Map;

/**
 * - in Jaeger, these are Longs, so logging them as such is far more efficient for space in elastic
 * - in Zipkin they are strings, so leave them as such
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class TracingJsonLogEnhancer implements JsonLogEnhancer {
	@Override
	public int getMapPriority() {
		return 10;
	}

	private void remapLong(String key, Map<String, String> context, Map<String, Object> log) {
		String val = context.remove(key);
		if (val != null) {
			log.put(key, Long.parseLong(val));
		}
	}

	@Override
	public void map(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects) {
		// no special processing required.
	}

	@Override
	public void failed(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects, Throwable e) {
	}
}
