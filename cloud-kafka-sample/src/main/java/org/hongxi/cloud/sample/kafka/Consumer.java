package org.hongxi.cloud.sample.kafka;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

	private static final Logger log = LoggerFactory.getLogger(Consumer.class);

	private final List<SampleMessage> messages = new CopyOnWriteArrayList<>();

	@KafkaListener(topics = "${app.kafka.topic}")
	void processMessage(SampleMessage message) {
		this.messages.add(message);
		log.info("Received sample message [{}]", message);
	}

	public List<SampleMessage> getMessages() {
		return this.messages;
	}

}