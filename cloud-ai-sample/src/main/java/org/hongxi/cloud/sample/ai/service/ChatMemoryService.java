package org.hongxi.cloud.sample.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

/**
 * ChatMemory 多轮对话服务
 * <p>
 * 演示基于 JDBC（PostgreSQL）持久化的对话记忆能力：
 * <ul>
 *   <li>通过 {@link MessageWindowChatMemory} 管理滑动窗口大小的对话历史</li>
 *   <li>通过 {@link MessageChatMemoryAdvisor} 自动拦截 ChatClient 请求/响应，完成记忆的加载与保存</li>
 *   <li>不同 conversationId 对应独立的对话上下文，实现会话隔离</li>
 * </ul>
 * </p>
 *
 * @author javahongxi
 */
@Service
public class ChatMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryService.class);

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatMemoryService(ChatClient.Builder chatClientBuilder,
                             ChatMemoryRepository chatMemoryRepository) {
        // 构建基于 JDBC 的对话记忆（滑动窗口保留最近 20 条消息）
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();

        // 通过 Advisor 机制自动管理对话历史：
        // 请求前 → 从 DB 加载历史消息并注入 Prompt
        // 响应后 → 将本轮 user + assistant 消息写回 DB
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * 带记忆的多轮对话
     *
     * @param conversationId 会话 ID（相同 ID 共享对话上下文）
     * @param userMessage    用户输入
     * @return AI 回复内容
     */
    public String chat(String conversationId, String userMessage) {
        log.info("ChatMemory 对话，conversationId={}, message={}", conversationId, userMessage);
        return chatClient.prompt()
                .user(userMessage)
                // 通过 advisors() 传入 conversationId，供 MessageChatMemoryAdvisor 识别会话
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    /**
     * 清除指定会话的历史记录
     *
     * @param conversationId 会话 ID
     */
    public void clearMemory(String conversationId) {
        chatMemory.clear(conversationId);
        log.info("已清除会话记忆，conversationId={}", conversationId);
    }
}
