package org.hongxi.cloud.sample.seata.business;

import feign.RequestInterceptor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.hongxi.cloud.sample.api.CloudConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import static org.apache.dubbo.common.constants.CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME;

@SpringBootApplication(exclude = {
        org.apache.dubbo.spring.boot.autoconfigure.observability.otel.OpenTelemetryAutoConfiguration.class
})
@EnableFeignClients
@EnableScheduling
public class BusinessApplication {

    public static void main(String[] args) {
        System.setProperty(DUBBO_PREFER_JSON_FRAMEWORK_NAME, CloudConstants.FASTJSON2);
        SpringApplication.run(BusinessApplication.class, args);
    }

    @Bean
    @LoadBalanced
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

    @FeignClient("storage-service")
    public interface StorageService {

        @GetMapping(path = "/storage/{commodityCode}/{count}")
        String storage(@PathVariable("commodityCode") String commodityCode,
                @PathVariable("count") int count);
    }

    @FeignClient("order-service")
    public interface OrderService {

        @PostMapping(path = "/order")
        String order(@RequestParam("userId") String userId,
                @RequestParam("commodityCode") String commodityCode,
                @RequestParam("orderCount") int orderCount);
    }

}
