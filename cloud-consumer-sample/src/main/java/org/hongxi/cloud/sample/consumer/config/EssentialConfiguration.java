package org.hongxi.cloud.sample.consumer.config;

import com.alibaba.cloud.sentinel.annotation.SentinelRestTemplate;
import feign.RequestInterceptor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Created by javahongxi on 2026/6/1.
 */
@Configuration(proxyBeanMethods = false)
public class EssentialConfiguration {

    @Bean
    @LoadBalanced
    @SentinelRestTemplate(
            blockHandler = "handleException", blockHandlerClass = SentinelExceptionHandler.class,
            fallback = "handleFallback", fallbackClass = SentinelExceptionHandler.class,
            urlCleaner = "cleanUrl", urlCleanerClass = SentinelExceptionHandler.class
    )
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public RequestInterceptor feignTracingInterceptor(Tracer tracer) {
        return template -> {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                String traceparent = String.format("00-%s-%s-01",
                        currentSpan.context().traceId(),
                        currentSpan.context().spanId());
                template.header("traceparent", traceparent);
            }
        };
    }
}
