package org.hongxi.cloud.sample.seata.business.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义业务指标配置
 * <p>
 * 通过 Prometheus 端点暴露，演示 Micrometer 自定义指标能力。
 * 跟踪 Seata 分布式事务的提交与回滚次数。
 */
@Configuration(proxyBeanMethods = false)
public class MetricsConfig {

    @Bean
    public Counter seataTransactionCommitted(MeterRegistry registry) {
        return Counter.builder("seata.transaction.committed.total")
                .description("Total Seata distributed transactions committed")
                .register(registry);
    }

    @Bean
    public Counter seataTransactionRolledBack(MeterRegistry registry) {
        return Counter.builder("seata.transaction.rolled_back.total")
                .description("Total Seata distributed transactions rolled back")
                .register(registry);
    }
}
