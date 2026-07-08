package org.hongxi.cloud.sample.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class Producer implements ApplicationRunner {

	private final KafkaTemplate<Object, SampleMessage> kafkaTemplate;

	@Value("${app.kafka.topic}")
	private String topic;

	Producer(KafkaTemplate<Object, SampleMessage> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		SampleMessage message = new SampleMessage(1, "test");
		this.kafkaTemplate.send(this.topic, message);
		System.out.println("Sent sample message [" + message + "] to topic [" + this.topic + "]");
	}
}