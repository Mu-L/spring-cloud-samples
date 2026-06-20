package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.AiChatService;
import org.hongxi.cloud.sample.ai.vo.PersonInfo;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * 简单聊天接口
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return aiChatService.chat(message);
    }

    /**
     * 流式聊天接口（SSE）
     */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chatStream(@RequestParam String message) {
        return aiChatService.chatStream(message);
    }

    /**
     * 结构化输出示例 - 返回 JSON 对象
     */
    @GetMapping("/extract")
    public PersonInfo extractPersonInfo(@RequestParam String text) {
        return aiChatService.extractPersonInfo(text);
    }
}
