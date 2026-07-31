package org.hongxi.cloud.sample.ai.rag.controller;

import org.hongxi.cloud.sample.ai.rag.service.LongTermMemoryService;
import org.hongxi.cloud.sample.ai.rag.vo.ChatRequest;
import org.hongxi.cloud.sample.ai.rag.vo.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 长期记忆对话控制器
 * <p>
 * 提供基于向量数据库长期记忆的 REST 接口：
 * <ul>
 *   <li>POST /ai/long-term-memory/chat — 带长期记忆的多轮对话</li>
 *   <li>DELETE /ai/long-term-memory/{conversationId} — 清除指定会话的短期记忆</li>
 * </ul>
 * 与 /ai/memory/chat 的区别：本接口额外通过 VectorStoreChatMemoryAdvisor
 * 从向量库中按语义相似度检索历史对话，实现跨会话的长期记忆能力。
 * </p>
 *
 * @author javahongxi
 */
@RestController
@RequestMapping("/ai/long-term-memory")
public class LongTermMemoryController {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryController.class);

    private final LongTermMemoryService longTermMemoryService;

    public LongTermMemoryController(LongTermMemoryService longTermMemoryService) {
        this.longTermMemoryService = longTermMemoryService;
    }

    /**
     * 带长期记忆的多轮对话
     * <p>
     * 短期记忆（JDBC 滑动窗口）保证当前会话连贯，
     * 长期记忆（VectorStore）按语义相似度检索历史，补充到 system prompt。
     * </p>
     * <p>
     * 示例请求体：
     * <pre>
     * {
     *   "conversationId": "session-001",
     *   "message": "我叫小明，请记住"
     * }
     * </pre>
     *
     * @param request 包含 conversationId 和 message
     * @return AI 回复（包含 conversationId 和回复内容）
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null ? request.conversationId() : "default";
        log.info("长期记忆对话请求，conversationId={}", conversationId);
        String reply = longTermMemoryService.chat(conversationId, request.message());
        return ResponseEntity.ok(new ChatResponse(conversationId, reply));
    }

    /**
     * 清除指定会话的短期记忆
     * <p>
     * 注意：只清除 JDBC 中的短期滑动窗口记忆，
     * 向量库中的长期记忆不会被清除（会随语义检索自然淘汰）。
     * </p>
     *
     * @param conversationId 会话 ID
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<String> clearMemory(@PathVariable String conversationId) {
        log.info("清除长期记忆会话的短期记忆，conversationId={}", conversationId);
        longTermMemoryService.clearMemory(conversationId);
        return ResponseEntity.ok("短期会话记忆已清除（长期向量记忆保留）");
    }
}
