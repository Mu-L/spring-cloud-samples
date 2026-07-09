package org.hongxi.cloud.sample.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.ShareAcknowledgment;
import org.springframework.stereotype.Component;

/**
 * Share Consumer 演示 - 展示 Kafka 4.x Share Groups (KIP-932) 的两种确认模式
 * <p>
 * 与传统 Consumer Group 不同，Share Group 中的多个消费者可以并行消费同一分区的消息，
 * 非常适合任务分发和工作队列场景。
 */
@Component
public class ShareConsumer {

	/**
	 * 隐式确认模式 - 方法正常返回自动 ACCEPT，抛出异常自动 REJECT
	 */
	@KafkaListener(
			topics = "${app.kafka.share-topic}",
			containerFactory = "implicitShareKafkaListenerContainerFactory",
			groupId = "${app.kafka.share-group}"
	)
	void processImplicit(ConsumerRecord<String, SampleMessage> record) {
		System.out.println("[Share-Implicit] Received: " + record.value()
				+ " from partition " + record.partition()
				+ " offset " + record.offset());
	}

	/**
	 * 显式确认模式 - 手动控制每条消息的确认
	 * <p>
	 * acknowledge() = ACCEPT，处理成功
	 * release()     = RELEASE，临时失败，消息将被重新投递
	 * reject()      = REJECT，永久失败，不再重试
	 */
	@KafkaListener(
			topics = "${app.kafka.share-topic-explicit}",
			containerFactory = "explicitShareKafkaListenerContainerFactory",
			groupId = "${app.kafka.share-group}",
			concurrency = "5"
	)
	void processExplicit(ConsumerRecord<String, SampleMessage> record, ShareAcknowledgment acknowledgment) {
		System.out.println("[Share-Explicit] Received: " + record.value()
				+ " from partition " + record.partition()
				+ " offset " + record.offset());
		try {
			// 模拟业务处理
			if (record.value().getId() % 5 == 0) {
				System.out.println("[Share-Explicit] Simulating retry for id=" + record.value().getId());
				acknowledgment.release();
				return;
			}
			acknowledgment.acknowledge();
		} catch (Exception e) {
			acknowledgment.reject();
		}
	}
}
