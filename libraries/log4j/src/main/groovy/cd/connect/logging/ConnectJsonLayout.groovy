package cd.connect.logging

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.impl.ThrowableProxy
import org.apache.logging.log4j.core.layout.AbstractLayout

import java.text.SimpleDateFormat

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Plugin(
  name = "ConnectJsonLayout",
  category = "Core",
  elementType = "layout",
  printObject = true
)
@CompileStatic
class ConnectJsonLayout extends AbstractLayout<LogEvent> {
  protected List<JsonLogEnhancer> loggingProcessors = EnhancerServiceLoader.findJsonLogEnhancers()
	protected boolean prettyPrint;
	protected String prettyPrintSuffix;

  ConnectJsonLayout() {
    super(null, null, null) // nothing accepted

	  prettyPrint = System.getProperty("connect.layout.pretty") != null;
	  prettyPrintSuffix = System.getProperty("connect.layout.pretty", "")
  }

  @Override
  byte[] toByteArray(LogEvent logEvent) {
    Map<String, Object> jsonContext = [:]
    List<String> alreadyEncodedJsonObjects = []
    Map<String, String> processContext = [:]

    // copy data out of read only map from ThreadContext (i.e. MDC)
    processContext.putAll(logEvent.contextData.toMap())

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
      jsonContext["@timestamp"] = sdf.format(new Date(logEvent.timeMillis));

      jsonContext.message = logEvent.message.formattedMessage
      jsonContext.priority = logEvent.level.toString()
      jsonContext.path = logEvent.loggerName
      jsonContext.thread = logEvent.threadName

      if (logEvent.source) {
        jsonContext["class"] = logEvent.source.className
        jsonContext.file = logEvent.source.fileName + ":" + logEvent.source.lineNumber
        jsonContext.method = logEvent.source.methodName
      }

      if (logEvent.thrownProxy) {
        jsonContext.stack_trace = prettyPrintStackTrace(logEvent.thrownProxy)
      }

      loggingProcessors.forEach({ JsonLogEnhancer p ->
        p.map(processContext, jsonContext, alreadyEncodedJsonObjects);
      });

      String json = JsonOutput.toJson(jsonContext)
      if (alreadyEncodedJsonObjects) {
        json = json.substring(0, json.length()-1) + "," + alreadyEncodedJsonObjects.join(',') + '}'
      }

	    if (prettyPrint) {
		    json = JsonOutput.prettyPrint(json) + prettyPrintSuffix
	    }

      return (json + "\n").bytes
    } catch (Exception ex) {
      try {
        loggingProcessors.forEach({ JsonLogEnhancer p -> p.failed(processContext, jsonContext, alreadyEncodedJsonObjects, ex)})
      } finally {
        ex.printStackTrace()
      }
    }

    return new byte[0]
  }

  // extracted from key bits of the log4j2 libs
  private static String prettyPrintStackTrace(ThrowableProxy throwable) {
    String offset = "\n\t"
    def result = "${throwable.name}: ${throwable.message}${offset}${throwable.extendedStackTrace.join(offset)}"

    ThrowableProxy cause = throwable.causeProxy

    while (cause) {
      result += "${offset}Caused by: ${cause.name}: ${cause.message}${offset}${cause.extendedStackTrace.join(offset)}"
      cause = cause.causeProxy
    }

    return result
  }

  @Override
  LogEvent toSerializable(LogEvent logEvent) {
    return logEvent
  }

  @Override
  String getContentType() {
    return 'application/octet-stream'
  }

	@PluginFactory
  static ConnectJsonLayout createLayout() {
    return new ConnectJsonLayout();
  }
}
