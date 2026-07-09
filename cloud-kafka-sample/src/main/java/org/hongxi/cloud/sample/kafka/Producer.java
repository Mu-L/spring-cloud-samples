package org.hongxi.cloud.sample.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class Producer {

	private final KafkaTemplate<Object, SampleMessage> kafkaTemplate;

	@Value("${app.kafka.topic}")
	private String topic;

	@Value("${app.kafka.share-topic}")
	private String shareTopic;

	@Value("${app.kafka.share-topic-explicit}")
	private String shareTopicExplicit;

	Producer(KafkaTemplate<Object, SampleMessage> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void sendTraditional() {
		SampleMessage message = new SampleMessage(1, "test");
		this.kafkaTemplate.send(this.topic, message);
		System.out.println("Sent sample message [" + message + "] to topic [" + this.topic + "]");
	}

	public void sendShareImplicit(int count) {
		for (int i = 1; i <= count; i++) {
			SampleMessage shareMsg = new SampleMessage(i, "share-task-" + i);
			this.kafkaTemplate.send(this.shareTopic, shareMsg);
			System.out.println("Sent share message [" + shareMsg + "] to topic [" + this.shareTopic + "]");
		}
	}

	public void sendShareExplicit(int count) {
		for (int i = 1; i <= count; i++) {
			SampleMessage explicitMsg = new SampleMessage(i, "explicit-task-" + i);
			this.kafkaTemplate.send(this.shareTopicExplicit, explicitMsg);
			System.out.println("Sent explicit share message [" + explicitMsg + "] to topic [" + this.shareTopicExplicit + "]");
		}
	}
}