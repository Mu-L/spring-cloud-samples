package org.hongxi.cloud.sample.consumer;

import io.grpc.NameResolverRegistry;
import org.hongxi.cloud.grpc.discovery.DiscoveryClientNameResolverProvider;
import org.hongxi.cloud.sample.api.CloudConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.ImportGrpcClients;

import static org.apache.dubbo.common.constants.CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME;

/**
 * Created by javahongxi on 2026/6/1.
 */
@SpringBootApplication(exclude = {
    org.apache.dubbo.spring.boot.autoconfigure.observability.otel.OpenTelemetryAutoConfiguration.class
})
@EnableFeignClients
@ImportGrpcClients(basePackages = "org.hongxi.cloud.sample.idl.unary")
public class ConsumerApplication {
    public static void main(String[] args) {
        // org.apache.dubbo.common.utils.JsonUtils
        System.setProperty(DUBBO_PREFER_JSON_FRAMEWORK_NAME, CloudConstants.FASTJSON2);
        SpringApplication.run(ConsumerApplication.class, args);
    }

    /**
     * 将 NameResolverProvider 注册到 gRPC 全局注册表。
     * <p>
     * 必须确保 nameResolverProvider 先于 stub bean 实例化
     * <p>
     */
    @Bean
    DiscoveryClientNameResolverProvider nameResolverProvider(DiscoveryClient discoveryClient) {
        var provider = new DiscoveryClientNameResolverProvider(discoveryClient);
        NameResolverRegistry.getDefaultRegistry().register(provider);
        return provider;
    }
}
