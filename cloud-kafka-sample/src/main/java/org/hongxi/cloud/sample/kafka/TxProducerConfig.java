package org.hongxi.cloud.sample.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

/**
 * Kafka 生产者配置
 * <p>
 * 显式创建非事务 KafkaTemplate 和事务 KafkaTemplate，避免 Spring Boot 自动配置
 * 因检测到已有 KafkaTemplate Bean 而跳过创建默认非事务模板。
 */
@Configuration
public class TxProducerConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	/**
	 * 非事务 KafkaTemplate，用于传统消息和 Share Group 消息发送
	 */
	@Bean
	@Qualifier("defaultKafkaTemplate")
	public KafkaTemplate<Object, SampleMessage> defaultKafkaTemplate() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
		ProducerFactory<Object, SampleMessage> pf = new DefaultKafkaProducerFactory<>(props);
		return new KafkaTemplate<>(pf);
	}

	/**
	 * 事务 KafkaTemplate，用于事务消息发送（配置 transactional.id）
	 */
	@Bean
	@Qualifier("txKafkaTemplate")
	public KafkaTemplate<Object, SampleMessage> txKafkaTemplate() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
		// 事务 ID，每个生产者实例会追加分区号作为唯一标识
		props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "kafka-sample-tx");
		ProducerFactory<Object, SampleMessage> pf = new DefaultKafkaProducerFactory<>(props);
		return new KafkaTemplate<>(pf);
	}
}
