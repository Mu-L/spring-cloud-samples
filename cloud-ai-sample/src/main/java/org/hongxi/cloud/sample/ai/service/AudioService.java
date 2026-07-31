package org.hongxi.cloud.sample.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.stereotype.Service;

/**
 * 音频服务（TTS）
 * <p>
 * 基于 Spring AI 的 {@link TextToSpeechModel}，
 * 底层通过 DashScope OpenAI 兼容接口调用 CosyVoice V3（TTS）模型。
 * </p>
 * <p>
 * 注意：DashScope 不支持 OpenAI 标准的 /v1/audio/transcriptions 端点，
 * 因此 STT（语音转文字）功能无法通过 Spring AI 的 TranscriptionModel 实现。
 * 如需 STT 能力，可考虑使用 DashScope 原生 API（如 paraformer-v2 / fun-asr）。
 * </p>
 *
 * @author javahongxi
 * @see TextToSpeechModel
 */
@Service
public class AudioService {

    private static final Logger log = LoggerFactory.getLogger(AudioService.class);

    private final TextToSpeechModel textToSpeechModel;

    public AudioService(TextToSpeechModel textToSpeechModel) {
        this.textToSpeechModel = textToSpeechModel;
    }

    /**
     * 文字转语音（TTS）
     *
     * @param text  要转换的文本
     * @param voice 语音角色（可选，如 longanyang / longanhuan），null 使用默认
     * @return 音频字节数据（MP3 格式）
     */
    public byte[] textToSpeech(String text, String voice) {
        log.info("TTS 请求, text length: {}, voice: {}", text.length(), voice);

        if (voice != null && !voice.isBlank()) {
            TextToSpeechOptions options = TextToSpeechOptions.builder()
                    .voice(voice)
                    .build();
            TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, options);
            return textToSpeechModel.call(prompt).getResult().getOutput();
        }
        return textToSpeechModel.call(text);
    }
}
