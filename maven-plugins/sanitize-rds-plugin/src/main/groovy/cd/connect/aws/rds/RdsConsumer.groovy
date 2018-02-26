package cd.connect.aws.rds

import com.amazonaws.AmazonClientException
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean

/**
 * from AWS SQS Async example
 *
 * Created by Richard Vowles on 16/02/18.
 */
@CompileStatic
class RdsConsumer extends Thread {
	private Logger log = LoggerFactory.getLogger(getClass())
	final AmazonSQS sqsClient
	final String queueUrl
	final AtomicBoolean stop
	final List<Closure> listeners = []

	RdsConsumer(AmazonSQS sqsClient, String queueUrl, AtomicBoolean stop) {
		this.sqsClient = sqsClient;
		this.queueUrl = queueUrl;
		this.stop = stop
	}

	void addListener(Closure listener) {
		listeners.add(listener)
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	private decodeMessage(String body) {
		def json = new JsonSlurper().parseText(body)

		json = new JsonSlurper().parseText(json.Message)

		RdsEvent event = new RdsEvent()

		if (json['Event Source'] == 'db-instance') {
			String database = json['Source ID']
			String eventId = json['Event ID']
			if (eventId.contains('#')) {
				eventId = eventId.substring(eventId.lastIndexOf('#') + 1)
			}

			event.source = RdsEvent.SourceType.INSTANCE
			event.name = database
			event.event = RdsEvent.EventType.eventFromString(eventId)

			println "database instance event for db: ${database} -> id ${eventId}"
		}

		if (event.source && event.event) {
			// everything that returns true uses the event and processes it, so keep that list
			List<Closure> removals = listeners.findAll({return it(event)})
			// remove them from the listeners
			listeners.removeAll(removals)
		}
	}


	void run() {
		try {
			while (!stop.get()) {
				try {
					final ReceiveMessageResult result = sqsClient
						.receiveMessage(new
						ReceiveMessageRequest(queueUrl));

					if (!result.getMessages().isEmpty()) {
						result.getMessages().each { Message m ->
//							println m.body

							decodeMessage(m.body)
						}

//						sqsClient.deleteMessage(new
//							DeleteMessageRequest(queueUrl,
//							m.getReceiptHandle()));
					}
				} catch (AmazonClientException e) {
					log.error(e.getMessage());
				}
			}
		} catch (AmazonClientException e) {
			/*
			 * By default, AmazonSQSClient retries calls 3 times before
			 * failing. If this unlikely condition occurs, stop.
			 */
			log.error("Consumer: " + e.getMessage());
			System.exit(1);
		}
	}
}
