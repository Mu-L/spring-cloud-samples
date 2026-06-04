package org.hongxi.cloud.sample.provider.dubbo;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by javahongxi on 2026/6/1.
 */
@SpringBootApplication(exclude = {
        org.apache.dubbo.spring.boot.autoconfigure.observability.otel.OpenTelemetryAutoConfiguration.class
})
@EnableDubbo
public class DubboProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(DubboProviderApplication.class, args);
    }
}
