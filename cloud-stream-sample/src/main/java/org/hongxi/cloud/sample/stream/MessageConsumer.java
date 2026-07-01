package org.hongxi.cloud.sample.stream;

import java.util.function.Consumer;
import java.util.function.Function;

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

}
