package org.hongxi.cloud.sample.gateway;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

/**
 * Created by javahongxi on 2026/6/1.
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public GlobalFilter metricsFilter(Counter gatewayRequestsTotal, Timer gatewayResponseTime) {
        return (exchange, chain) -> {
            gatewayRequestsTotal.increment();
            long start = System.nanoTime();
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.nanoTime() - start;
                gatewayResponseTime.record(java.time.Duration.ofNanos(duration));
            }));
        };
    }
}
