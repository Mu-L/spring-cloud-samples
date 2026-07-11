package org.hongxi.cloud.sample.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;

import java.util.function.Supplier;

/**
 * Spring Cloud Stream 示例应用
 * <p>
 * 演示以下核心场景：
 * <ol>
 *   <li>StreamBridge 编程式消息发布</li>
 *   <li>Supplier 定时消息源</li>
 *   <li>Function 消息处理管道（见 MessageConsumer#transform）</li>
 *   <li>Consumer / Function / Supplier 三种函数式编程模型</li>
 * </ol>
 *
 * @author javahongxi
 */
@SpringBootApplication
public class StreamApplication {

    private static final Logger log = LoggerFactory.getLogger(StreamApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(StreamApplication.class, args);
    }

    /**
     * 启动时通过 StreamBridge 发送一条消息到 stream-demo-topic
     * <p>
     * 演示 StreamBridge 的编程式消息发布能力
     */
    @Bean
    CommandLineRunner runner(StreamBridge streamBridge) {
        return args -> {
            boolean result = streamBridge.send("output-out-0", "Hello");
            log.info("Send message: {}, result: {}", "Hello", result);
        };
    }

    /**
     * 定时消息源 - 每隔1秒自动发送消息到 stream-demo-topic2
     * <p>
     * 演示 Supplier 函数式消息源，Spring Cloud Stream 会自动以固定间隔调用该 Supplier
     */
    @Bean
    Supplier<String> output2() {
        return () -> {
            String value = "你好";
            log.info("发送消息: {}", value);
            return value;
        };
    }
}
