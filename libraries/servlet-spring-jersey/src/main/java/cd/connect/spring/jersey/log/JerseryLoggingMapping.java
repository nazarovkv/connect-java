package cd.connect.spring.jersey.log;

import cd.connect.logging.JsonLogEnhancer;
import cd.connect.spring.jersey.JerseyLoggerPoint;
import org.glassfish.jersey.logging.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class JerseryLoggingMapping implements JsonLogEnhancer {
	@Override
	public int getMapPriority() {
		return 30;
	}

	/**
	 * 1) Swaps message = jersey.payload.
	 * 2) If REST_CONTEXT exists, it swaps it into the message, otherwise it is just deleted.
	 * 3) If REST_STATUS_CODe exists, put it in jersey.statusCode, otherwise it is just deleted.
	 *
	 */

	@Override
	public void map(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects) {
		String restContent = context.remove(Constants.REST_CONTEXT);
		String statusCode = context.remove(Constants.REST_STATUS_CODE);

		if (JerseyLoggerPoint.LOGGER_POINT.equals(log.get("path"))) {
			Map<String, Object> jersey = new HashMap<>();

			log.put("priority", "REST"); // overwrite the priority

			jersey.put("payload", log.remove("message"));

			if (restContent != null) {
				log.put("message", restContent);
			}

			if (statusCode != null) {
				jersey.put("statusCode", Integer.parseInt(statusCode));
			}

			log.put("jersey", jersey);
		}
	}

	@Override
	public void failed(Map<String, String> context, Map<String, Object> log, List<String> alreadyEncodedJsonObjects, Throwable e) {
	}
}
