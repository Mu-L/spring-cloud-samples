package org.hongxi.cloud.sample.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI 配置类
 * <p>
 * 定义不同场景的 ChatClient：
 * - 默认的 chatClient 使用 OpenAI（DashScope）模型
 * - visionChatClient 使用支持多模态的模型，用于图像识别
 * - deepSeekChatClient 使用 DeepSeek 模型
 * </p>
 *
 * @author javahongxi
 */
@Configuration
public class AiConfig {

    /**
     * 标记 OpenAI ChatModel 为 Primary，解决多 Provider 共存时 ChatClient.Builder 的 Bean 歧义
     */
    @Bean
    @Primary
    public ChatModel primaryChatModel(OpenAiChatModel openAiChatModel) {
        return openAiChatModel;
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

    /**
     * DeepSeek ChatClient
     */
    @Bean
    public ChatClient deepSeekChatClient(ChatClient.Builder builder,
                                         DeepSeekChatModel deepSeekChatModel) {
        return builder
                .defaultOptions(OpenAiChatOptions.builder().model(deepSeekChatModel.getOptions().getModel()))
                .build();
    }
}
