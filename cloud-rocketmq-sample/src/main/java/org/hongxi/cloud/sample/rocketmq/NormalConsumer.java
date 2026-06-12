package org.hongxi.cloud.sample.rocketmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.stereotype.Service;

/**
 * RocketMQ 普通消息消费者实现类
 */
@Slf4j
@Service
@RocketMQMessageListener(topic = "demo-normal-topic", consumerGroup = "my-consumer_demo-normal-topic")
public class NormalConsumer implements RocketMQListener {

    @Override
    public ConsumeResult consume(MessageView messageView) {
        log.info("received message: {}", messageView);
        return ConsumeResult.SUCCESS;
    }
}