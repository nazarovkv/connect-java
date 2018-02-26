package cd.connect.aws.rds

import groovy.transform.CompileStatic

/**
 * Created by Richard Vowles on 16/02/18.
 */
@CompileStatic
class RdsEvent {
	enum SourceType {
		INSTANCE,
		SNAPSHOT
	}

	enum EventType {
		INSTANCE_DELETED(3),
		INSTANCE_STOPPED(87)

		private int code

		EventType(code) {
			this.code = code
		}

		static eventFromString(String code) {
			int c = Integer.parseInt(code.substring(code.lastIndexOf("-") + 1))

			return values().find({ it.code == c})
		}
	}

	String name
	SourceType source
	EventType event
}
