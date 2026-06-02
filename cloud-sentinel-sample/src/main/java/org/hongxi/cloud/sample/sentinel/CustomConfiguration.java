package org.hongxi.cloud.sample.sentinel;

import com.alibaba.cloud.circuitbreaker.sentinel.SentinelCircuitBreakerFactory;
import com.alibaba.cloud.circuitbreaker.sentinel.SentinelConfigBuilder;
import com.alibaba.cloud.sentinel.annotation.SentinelRestTemplate;
import com.alibaba.cloud.sentinel.rest.SentinelClientHttpResponse;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
public class CustomConfiguration {

    @Bean
    @SentinelRestTemplate(blockHandler = "handleException", blockHandlerClass = ExceptionUtil.class)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Converter<String, List<FlowRule>> converter() {
        return source -> JSON.parseObject(source, new TypeReference<>() {});
    }

    @Bean
    public Customizer<SentinelCircuitBreakerFactory> defaultConfig() {
        return factory -> {
            factory.configureDefault(
                    id -> new SentinelConfigBuilder().resourceName(id)
                            .rules(Collections.singletonList(new DegradeRule(id)
                                    .setGrade(RuleConstant.DEGRADE_GRADE_RT).setCount(100)
                                    .setTimeWindow(10)))
                            .build());
        };
    }

    public static class ExceptionUtil {

        private ExceptionUtil() {
        }

        public static SentinelClientHttpResponse handleException(
                HttpRequest request, byte[] body, ClientHttpRequestExecution execution, BlockException e) {
            log.info("Oops: {}", e.getClass().getCanonicalName());
            return new SentinelClientHttpResponse("custom block info");
        }
    }

    @Configuration
    @EnableWebMvc
    public static class WebMvcConfiguration implements WebMvcConfigurer {

        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            registry.addViewController("/errorPage").setViewName("errorPage");
        }
    }
}
