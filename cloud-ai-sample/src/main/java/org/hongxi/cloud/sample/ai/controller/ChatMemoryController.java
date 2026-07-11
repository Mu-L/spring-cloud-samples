package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.ChatMemoryService;
import org.hongxi.cloud.sample.ai.vo.ChatRequest;
import org.hongxi.cloud.sample.ai.vo.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ChatMemory 多轮对话控制器
 * <p>
 * 提供基于 JDBC 持久化对话记忆的 REST 接口：
 * <ul>
 *   <li>POST /ai/memory/chat  — 带记忆的多轮对话（相同 conversationId 共享上下文）</li>
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
     * 带记忆的多轮对话
     * <p>
     * 示例请求体：
     * <pre>
     * {
     *   "conversationId": "session-001",
     *   "message": "我想学习 Spring AI"
     * }
     * </pre>
     * 相同 conversationId 的请求会共享对话上下文，AI 能"记住"之前的对话内容。
     *
     * @param request 包含 conversationId 和 message
     * @return AI 回复（包含 conversationId 和回复内容）
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null ? request.conversationId() : "default";
        log.info("ChatMemory 对话请求，conversationId={}", conversationId);
        String reply = chatMemoryService.chat(conversationId, request.message());
        return ResponseEntity.ok(new ChatResponse(conversationId, reply));
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
