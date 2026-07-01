package org.hongxi.cloud.sample.stream;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stream 消息处理函数配置
 * <p>
 * 演示 Spring Cloud Stream 的函数式编程模型：
 * <ul>
 *   <li>Consumer - 终端消费者，接收并处理消息</li>
 *   <li>Function - 消息处理管道，接收消息、转换后输出到新 Topic</li>
 *   <li>Supplier - 定时消息源，自动生成消息（见 StreamApplication）</li>
 * </ul>
 *
 * @author hongxi
 */
@Configuration
public class MessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);

    /**
     * 基础消费者 - 消费 stream-demo-topic 的消息
     * <p>
     * 场景：启动时 StreamBridge 发送的 "Hello" 消息由此 Consumer 接收
     */
    @Bean
    public Consumer<String> input() {
        return message -> log.info("Received message: {}", message);
    }

    /**
     * 定时消息消费者 - 消费 stream-demo-topic2 的消息
     * <p>
     * 场景：Supplier output2 每隔1秒发送 "你好"，由此 Consumer 接收
     */
    @Bean
    public Consumer<String> input2() {
        return message -> log.info("收到消息: {}", message);
    }

    /**
     * 消息转换函数 - 将消息转为大写并添加处理标记
     * <p>
     * 场景：演示消息处理管道（Processing Pipeline）模式
     * <p>
     * 消息流: transform-in-0 → [toUpperCase + tag] → transform-out-0
     */
    @Bean
    public Function<String, String> transform() {
        return message -> {
            String result = "[PROCESSED] " + message.toUpperCase();
            log.info("消息转换: {} -> {}", message, result);
            return result;
        };
    }

    /**
     * 延迟消息消费者 - 消费 stream-delay-topic 的延迟消息
     * <p>
     * 场景：REST API 发送延迟消息，观察消息在指定延迟后被消费
     */
    @Bean
    public Consumer<String> delay() {
        return message -> log.info("[延迟消息] 收到: {} (时间: {})", message, java.time.LocalTime.now());
    }

    /**
     * 顺序消息消费者 - 消费 stream-fifo-topic 的顺序消息
     * <p>
     * 场景：REST API 发送带相同 ORDER_KEY 的消息，观察消息按发送顺序被消费
     */
    @Bean
    public Consumer<String> fifo() {
        return message -> log.info("[顺序消息] 收到: {} (时间: {})", message, java.time.LocalTime.now());
    }

    /**
     * Transform 管道发布器 - 为 REST API 提供 output binding 以通过 binder 发送消息
     * <p>
     * 该 Supplier 本身不产生消息（返回 null），仅用于创建 transformPublish-out-0 output binding，
     * 使 StreamController 可以通过 StreamBridge 经 binder（RocketMQ）发送消息到 stream-transform-topic，
     * 再由 transform 函数的 input binding 消费，避免直接向 input binding 发送导致的 WARN。
     */
    @Bean
    public Supplier<String> transformPublish() {
        return () -> null;
    }

    /**
     * 延迟消息发布器 - 创建 delayPublish-out-0 output binding
     * <p>
     * 延迟消息通过 StreamBridge 发送，在 Message header 中设置 DELAY_LEVEL 实现延迟投递，
     * 该 Supplier 仅用于创建 binding 和注册 Consumer 函数。
     */
    @Bean
    public Supplier<String> delayPublish() {
        return () -> null;
    }

    /**
     * 顺序消息发布器 - 创建 fifoPublish-out-0 output binding
     * <p>
     * 使 StreamController 可以通过 StreamBridge 经 binder 发送顺序消息到 stream-fifo-topic，
     * 配合 partitionKeyExpression 将相同 ORDER_KEY 的消息路由到同一队列，保证顺序消费。
     */
    @Bean
    public Supplier<String> fifoPublish() {
        return () -> null;
    }

}
