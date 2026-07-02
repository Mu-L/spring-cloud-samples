package org.hongxi.cloud.sample.gateway.config;

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
@Configuration
public class MetricsConfig {

    @Bean
    public Counter gatewayRequestsTotal(MeterRegistry registry) {
        return Counter.builder("gateway.requests.total")
                .description("Total requests routed through gateway-sample")
                .register(registry);
    }

    @Bean
    public Timer gatewayResponseTime(MeterRegistry registry) {
        return Timer.builder("gateway.response.time")
                .description("Response time for gateway-sample routing")
                .register(registry);
    }
}
