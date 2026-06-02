package org.hongxi.cloud.sample.stream;

import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by javahongxi on 2026/6/1.
 */
@Slf4j
@Configuration
public class MessageConsumer {

    /**
     * 函数式消费者，自动绑定到 input-in-0 binding
     */
    @Bean
    public Consumer<String> input() {
        return message -> log.info("Received message: {}", message);
    }
}
