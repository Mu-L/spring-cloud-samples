package org.hongxi.cloud.sample.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ShareKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultShareConsumerFactory;
import org.springframework.kafka.core.ShareConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

/**
 * Kafka Share Consumer (KIP-932) 配置
 * <p>
 * Share Groups 突破了传统消费者组"一个分区只能被一个消费者消费"的限制，
 * 允许多个消费者从同一个分区并行消费不同的消息，支持逐条 ACK/NACK。
 */
@Configuration
public class ShareConsumerConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${app.kafka.share-group:share-demo-group}")
	private String shareGroupId;

	@Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
	private String trustedPackages;

	/**
	 * 隐式确认模式 - 消息处理成功自动 ACCEPT，处理异常自动 REJECT
	 */
	@Bean
	public ShareConsumerFactory<String, SampleMessage> implicitShareConsumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
		props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
		return new DefaultShareConsumerFactory<>(props);
	}

	@Bean
	public ShareKafkaListenerContainerFactory<String, SampleMessage> implicitShareKafkaListenerContainerFactory(
			ShareConsumerFactory<String, SampleMessage> implicitShareConsumerFactory) {
		return new ShareKafkaListenerContainerFactory<>(implicitShareConsumerFactory);
	}

	/**
	 * 显式确认模式 - 需要手动调用 acknowledge()/release()/reject()
	 */
	@Bean
	public ShareConsumerFactory<String, SampleMessage> explicitShareConsumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
		props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
		return new DefaultShareConsumerFactory<>(props);
	}

	@Bean
	public ShareKafkaListenerContainerFactory<String, SampleMessage> explicitShareKafkaListenerContainerFactory(
			ShareConsumerFactory<String, SampleMessage> explicitShareConsumerFactory) {
		ShareKafkaListenerContainerFactory<String, SampleMessage> factory =
				new ShareKafkaListenerContainerFactory<>(explicitShareConsumerFactory);
		factory.getContainerProperties().setShareAckMode(ContainerProperties.ShareAckMode.MANUAL);
		return factory;
	}
}
