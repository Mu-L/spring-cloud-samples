package org.hongxi.cloud.sample.consumer.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义业务指标配置
 * <p>
 * 通过 Prometheus 端点暴露，演示 Micrometer 自定义指标能力。
 */
@Configuration(proxyBeanMethods = false)
public class MetricsConfig {

    @Bean
    public Counter httpRequestsTotal(MeterRegistry registry) {
        return Counter.builder("consumer.http.requests.total")
                .description("Total HTTP requests received by consumer-sample")
                .register(registry);
    }

    @Bean
    public Timer httpResponseTime(MeterRegistry registry) {
        return Timer.builder("consumer.http.response.time")
                .description("HTTP response time in consumer-sample")
                .register(registry);
    }
}
