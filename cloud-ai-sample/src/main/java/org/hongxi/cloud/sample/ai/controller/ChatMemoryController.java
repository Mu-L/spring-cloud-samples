package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.ChatMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * ChatMemory 多轮对话控制器
 * <p>
 * 提供基于内存对话记忆的 REST 接口：
 * <ul>
 *   <li>GET /ai/memory/chat  — 带记忆的多轮对话（相同 conversationId 共享上下文）</li>
 *   <li>DELETE /ai/memory/{conversationId} — 清除指定会话的历史记忆</li>
 * </ul>
 * </p>
 *
 * @author javahongxi
 */
@RestController
@RequestMapping("/ai/memory")
public class ChatMemoryController {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryController.class);

    private final ChatMemoryService chatMemoryService;

    public ChatMemoryController(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

    /**
     * 带记忆的多轮对话（SSE 流式输出）
     * <p>
     * 示例：GET /ai/memory/chat?conversationId=session-001&message=我想学习 Spring AI
     * </p>
     * 相同 conversationId 的请求会共享对话上下文，AI 能"记住"之前的对话内容。
     *
     * @param conversationId 会话 ID（可选，默认 default）
     * @param message        用户消息
     * @return AI 回复（SSE 流式输出）
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam(defaultValue = "default") String conversationId,
                             @RequestParam String message) {
        log.info("ChatMemory 对话请求，conversationId={}", conversationId);
        return chatMemoryService.chat(conversationId, message);
    }

    /**
     * 清除指定会话的对话记忆
     *
     * @param conversationId 会话 ID
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<String> clearMemory(@PathVariable String conversationId) {
        log.info("清除会话记忆，conversationId={}", conversationId);
        chatMemoryService.clearMemory(conversationId);
        return ResponseEntity.ok("会话记忆已清除");
    }
}
