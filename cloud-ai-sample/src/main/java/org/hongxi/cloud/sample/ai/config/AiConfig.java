package org.hongxi.cloud.sample.ai.config;

import org.hongxi.cloud.sample.ai.model.DashScopeImageModel;
import org.hongxi.cloud.sample.ai.model.DashScopeTtsModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
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
     * DashScope TTS（文字转语音）实现
     * <p>
     * 实现 Spring AI 的 {@link TextToSpeechModel} 接口，封装 DashScope 原生 HTTP API，
     * 调用 CosyVoice V3 系列模型进行语音合成。
     * DashScope 不支持 OpenAI 标准的 /v1/audio/speech 端点，因此使用自定义实现。
     * </p>
     */
    @Bean
    public TextToSpeechModel textToSpeechModel(@Qualifier("externalRestTemplate") RestTemplate restTemplate,
                                               @Value("${spring.ai.openai.api-key}") String apiKey,
                                               @Value("${spring.ai.openai.audio.speech.model:cosyvoice-v3-plus}") String ttsModel,
                                               @Value("${spring.ai.openai.audio.speech.voice:longanyang}") String ttsVoice) {
        return new DashScopeTtsModel(restTemplate, apiKey, ttsModel, ttsVoice);
    }
}
