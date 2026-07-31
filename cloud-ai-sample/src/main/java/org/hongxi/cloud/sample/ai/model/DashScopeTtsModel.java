package org.hongxi.cloud.sample.ai.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.*;

/**
 * DashScope TTS（文字转语音）实现
 * <p>
 * 实现 Spring AI 的 {@link TextToSpeechModel} 接口，封装 DashScope 原生 HTTP API。
 * 调用 CosyVoice V3 系列模型进行语音合成。
 * </p>
 * <p>
 * DashScope 不支持 OpenAI 标准的 {@code /v1/audio/speech} 端点，
 * 因此需要自定义实现，使用 DashScope 原生 {@code /api/v1/services/audio/tts/SpeechSynthesizer} 端点。
 * </p>
 *
 * @author javahongxi
 * @see TextToSpeechModel
 */
public class DashScopeTtsModel implements TextToSpeechModel {

    private static final Logger log = LoggerFactory.getLogger(DashScopeTtsModel.class);

    private static final String TTS_URL = "https://dashscope.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String defaultModel;
    private final String defaultVoice;

    public DashScopeTtsModel(RestTemplate restTemplate, String apiKey, String defaultModel, String defaultVoice) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.defaultVoice = defaultVoice;
    }

    @Override
    public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
        TextToSpeechMessage message = prompt.getInstructions();
        TextToSpeechOptions options = prompt.getOptions();

        String text = message.getText();
        String model = (options != null && options.getModel() != null) ? options.getModel() : defaultModel;
        String voice = (options != null && options.getVoice() != null) ? options.getVoice() : defaultVoice;

        log.info("DashScope TTS 请求, model: {}, voice: {}, text length: {}", model, voice, text.length());

        // 构建 DashScope 原生 API 请求体
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("text", text);
        input.put("voice", voice);
        input.put("format", "mp3");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", input);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            // DashScope API 返回 JSON，包含音频下载 URL
            ResponseEntity<String> response = restTemplate.exchange(
                    TTS_URL, HttpMethod.POST, request, String.class);

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                throw new RuntimeException("DashScope TTS 返回空响应");
            }

            log.debug("DashScope TTS 响应: {}", responseBody);

            // 解析 JSON，提取 audio url
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode audioNode = root.path("output").path("audio");
            String audioUrl = audioNode.path("url").asText(null);

            if (audioUrl == null || audioUrl.isEmpty()) {
                // 可能直接返回了二进制数据（某些模型/版本）
                String data = audioNode.path("data").asText(null);
                if (data != null && !data.isEmpty()) {
                    byte[] audioBytes = Base64.getDecoder().decode(data);
                    log.info("TTS 合成成功(Base64), 音频大小: {} bytes", audioBytes.length);
                    Speech speech = new Speech(audioBytes);
                    return new TextToSpeechResponse(List.of(speech));
                }
                throw new RuntimeException("DashScope TTS 响应中未找到音频 URL, response: " + responseBody);
            }

            // 下载音频文件（使用 URI 避免 URL 中已编码字符被二次编码）
            log.info("从 URL 下载音频: {}", audioUrl);
            ResponseEntity<byte[]> audioResponse = restTemplate.getForEntity(URI.create(audioUrl), byte[].class);
            byte[] audio = audioResponse.getBody();

            if (audio == null || audio.length == 0) {
                throw new RuntimeException("下载的音频数据为空");
            }

            log.info("TTS 合成成功, 音频大小: {} bytes", audio.length);
            Speech speech = new Speech(audio);
            return new TextToSpeechResponse(List.of(speech));

        } catch (Exception e) {
            log.error("DashScope TTS 调用失败: {}", e.getMessage());
            throw new RuntimeException("语音合成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt) {
        // 非流式实现：直接调用 call 并包装为 Flux
        return Flux.just(call(prompt));
    }

    @Override
    public TextToSpeechOptions getOptions() {
        return TextToSpeechOptions.builder()
                .model(defaultModel)
                .voice(defaultVoice)
                .build();
    }
}
