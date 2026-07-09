package org.hongxi.cloud.sample.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class Producer {

	private static final Logger log = LoggerFactory.getLogger(Producer.class);

	private final KafkaTemplate<Object, SampleMessage> kafkaTemplate;

	private final KafkaTemplate<Object, SampleMessage> txKafkaTemplate;

	@Value("${app.kafka.topic}")
	private String topic;

	@Value("${app.kafka.share-topic}")
	private String shareTopic;

	@Value("${app.kafka.share-topic-explicit}")
	private String shareTopicExplicit;

	@Value("${app.kafka.tx-topic}")
	private String txTopic;

	Producer(@Qualifier("defaultKafkaTemplate") KafkaTemplate<Object, SampleMessage> kafkaTemplate,
			@Qualifier("txKafkaTemplate") KafkaTemplate<Object, SampleMessage> txKafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
		this.txKafkaTemplate = txKafkaTemplate;
	}

	public void sendTraditional() {
		SampleMessage message = new SampleMessage(1, "test");
		this.kafkaTemplate.send(this.topic, message);
		log.info("Sent sample message [{}] to topic [{}]", message, this.topic);
	}

	public void sendShareImplicit(int count) {
		for (int i = 1; i <= count; i++) {
			SampleMessage shareMsg = new SampleMessage(i, "share-task-" + i);
			this.kafkaTemplate.send(this.shareTopic, shareMsg);
			log.info("Sent share message [{}] to topic [{}]", shareMsg, this.shareTopic);
		}
	}

	public void sendShareExplicit(int count) {
		for (int i = 1; i <= count; i++) {
			SampleMessage explicitMsg = new SampleMessage(i, "explicit-task-" + i);
			this.kafkaTemplate.send(this.shareTopicExplicit, explicitMsg);
			log.info("Sent explicit share message [{}] to topic [{}]", explicitMsg, this.shareTopicExplicit);
		}
	}

	/**
	 * 事务消息 - 使用事务 KafkaTemplate 原子性发送多条消息
	 * <p>
	 * commit=true: 事务提交，消费者可读到消息
	 * commit=false: 事务回滚，消费者读不到消息（read_committed 隔离级别下）
	 */
	public void sendTransactional(int count, boolean commit) {
		try {
			this.txKafkaTemplate.executeInTransaction(operations -> {
				for (int i = 1; i <= count; i++) {
					SampleMessage txMsg = new SampleMessage(i, "tx-task-" + i);
					operations.send(this.txTopic, txMsg);
					log.info("[TX] Sent message [{}] to topic [{}]", txMsg, this.txTopic);
				}
				if (!commit) {
					log.info("[TX] Simulating transaction rollback, messages will NOT be visible to consumers");
					throw new RuntimeException("Simulated transaction rollback");
				}
				return null;
			});
			log.info("[TX] Transaction committed successfully, {} messages visible to consumers", count);
		} catch (Exception e) {
			log.warn("[TX] Transaction rolled back, messages are NOT visible to consumers: {}", e.getMessage());
		}
	}
}