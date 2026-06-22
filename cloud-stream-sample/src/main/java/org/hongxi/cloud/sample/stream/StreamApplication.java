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
 * Created by javahongxi on 2026/6/1.
 */
@SpringBootApplication
public class StreamApplication {

    private static final Logger log = LoggerFactory.getLogger(StreamApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(StreamApplication.class, args);
    }

    @Bean
    CommandLineRunner runner(StreamBridge streamBridge) {
        return args -> {
            boolean result = streamBridge.send("output-out-0", "Hello");
            log.info("Send message: {}, result: {}", "Hello", result);
        };
    }

    /**
     * 每隔1秒会自动发送消息
     * @return
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
