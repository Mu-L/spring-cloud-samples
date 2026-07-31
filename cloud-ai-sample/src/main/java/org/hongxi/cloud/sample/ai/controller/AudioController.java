package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.AudioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 音频控制器（TTS）
 * <p>
 * 基于 Spring AI 的 TextToSpeechModel，
 * 通过 DashScope OpenAI 兼容接口调用 CosyVoice V3（TTS）模型。
 * </p>
 * <ul>
 *   <li>GET /ai/audio/tts — 文字转语音，返回 MP3 音频</li>
 * </ul>
 *
 * @author javahongxi
 */
@RestController
@RequestMapping("/ai/audio")
public class AudioController {

    private final AudioService audioService;

    public AudioController(AudioService audioService) {
        this.audioService = audioService;
    }

    /**
     * 文字转语音（TTS）
     * <p>
     * 使用 DashScope CosyVoice V3 模型将文本转为语音，返回 MP3 音频流。
     * </p>
     *
     * @param text  要转换的文本
     * @param voice 语音角色（可选，如 longanyang / longanhuan）
     * @return MP3 音频
     */
    @GetMapping("/tts")
    public ResponseEntity<byte[]> textToSpeech(
            @RequestParam String text,
            @RequestParam(required = false) String voice) {

        byte[] audio = audioService.textToSpeech(text, voice);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentLength(audio.length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"");

        return ResponseEntity.ok().headers(headers).body(audio);
    }
}
