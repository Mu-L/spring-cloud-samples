package org.hongxi.cloud.sample.ai.config;

import org.hongxi.cloud.sample.ai.model.DashScopeImageModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * - multimodalChatClient 使用多模态模型（qwen3.7-plus），用于视觉理解和图像生成
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
     * 普通 RestTemplate，用于调用外部 API（如 DashScope）
     */
    @Bean
    public RestTemplate externalRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    /**
     * DashScope 文生图 ImageModel 实现
     * <p>
     * 实现 Spring AI 的 {@link ImageModel} 接口，封装 DashScope 原生异步 API，
     * 支持 wan2.7-image-pro 等模型。通过此 Bean，项目中所有图片生成逻辑均可
     * 通过统一的 {@code ImageModel.call(ImagePrompt)} 编程模型调用。
     * </p>
     */
    @Bean
    public DashScopeImageModel dashScopeImageModel(@Qualifier("externalRestTemplate") RestTemplate restTemplate,
                                                   @Value("${spring.ai.openai.api-key}") String apiKey,
                                                   @Value("${spring.ai.openai.image.model:wan2.7-image-pro}") String imageModel) {
        return new DashScopeImageModel(restTemplate, apiKey, imageModel);
    }

    /**
     * 多模态 ChatClient（视觉理解 + 图片生成）
     */
    @Bean
    public ChatClient multimodalChatClient(ChatClient.Builder builder,
                                       @Value("${spring.ai.multimodal.model:qwen3.7-plus}") String multimodalModel) {
        return builder
                .defaultOptions(OpenAiChatOptions.builder().model(multimodalModel))
                .build();
    }
}
