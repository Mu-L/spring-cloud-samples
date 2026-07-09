package org.hongxi.cloud.sample.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 事务消息消费者 - 只消费已提交事务的消息
 * <p>
 * 配合 TxConsumerConfig 中的 read_committed 隔离级别，
 * 未提交或已回滚的事务消息对消费者不可见。
 */
@Component
public class TxConsumer {

	private static final Logger log = LoggerFactory.getLogger(TxConsumer.class);

	@KafkaListener(
			topics = "${app.kafka.tx-topic}",
			containerFactory = "txKafkaListenerContainerFactory"
	)
	void processTxMessage(ConsumerRecord<String, SampleMessage> record) {
		log.info("[TX-Consumer] Received: {} from partition {} offset {}",
				record.value(), record.partition(), record.offset());
	}
}
