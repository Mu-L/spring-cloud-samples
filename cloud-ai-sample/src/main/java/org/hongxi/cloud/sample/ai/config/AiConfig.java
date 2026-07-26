package org.hongxi.cloud.sample.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AI 配置类
 * <p>
 * 定义不同场景的 ChatClient：
 * - 默认的 chatClient 使用 OpenAI（DashScope）模型
 * - visionChatClient 使用支持多模态的模型，用于图像识别
 * </p>
 *
 * @author javahongxi
 */
@Configuration
public class AiConfig {

    /**
     * 负载均衡 RestTemplate，通过服务名调用其他微服务模块
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    /**
     * 多模态视觉 ChatClient
     */
    @Bean
    public ChatClient visionChatClient(ChatClient.Builder builder,
                                       @Value("${spring.ai.vision.model:qwen3.7-plus}") String visionModel) {
        return builder
                .defaultOptions(OpenAiChatOptions.builder().model(visionModel))
                .build();
    }
}
