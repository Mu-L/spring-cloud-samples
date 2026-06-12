package org.hongxi.cloud.sample.consumer.dubbo;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.hongxi.cloud.sample.api.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication(exclude = {
        org.apache.dubbo.spring.boot.autoconfigure.observability.otel.OpenTelemetryAutoConfiguration.class
})
public class DubboConsumerApplication {

    @DubboReference
    private DemoService demoService;
    @Autowired
    private RocketMQClientTemplate rocketMQClientTemplate;

    public static void main(String[] args) {
        SpringApplication.run(DubboConsumerApplication.class, args);
    }

    @Bean
    CommandLineRunner runner() {
        return args -> {
            String result =demoService.sayHello("lily");
            log.info("invoke dubbo provider, result: {}", result);
            SendReceipt sendReceipt = rocketMQClientTemplate.syncSendNormalMessage("demo-normal-topic", "I'm normal message");
            log.info("send message, result: {}", sendReceipt);
        };
    }
}
