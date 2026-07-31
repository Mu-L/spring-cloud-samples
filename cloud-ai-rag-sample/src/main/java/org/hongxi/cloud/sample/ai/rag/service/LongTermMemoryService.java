package org.hongxi.cloud.sample.ai.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * 长期记忆对话服务
 * <p>
 * 组合两种记忆机制：
 * <ul>
 *   <li><b>短期记忆</b>（MessageWindowChatMemory + JDBC）：滑动窗口保留最近 20 条消息，
 *       保证当前会话上下文的连贯性</li>
 *   <li><b>长期记忆</b>（VectorStoreChatMemoryAdvisor + PGVector）：将历史对话向量化存入向量库，
 *       每次请求时按语义相似度检索相关历史，注入 system prompt，实现跨会话的长期记忆</li>
 * </ul>
 * 两个 Advisor 同时作用于 ChatClient，短期记忆提供即时上下文，长期记忆补充语义相关的历史信息。
 * </p>
 *
 * @author javahongxi
 */
@Service
public class LongTermMemoryService {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryService.class);

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public LongTermMemoryService(ChatClient.Builder chatClientBuilder,
                                 ChatMemoryRepository chatMemoryRepository,
                                 VectorStore vectorStore) {
        // 短期记忆：JDBC 持久化，滑动窗口保留最近 20 条消息
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();

        // 组合两个 Advisor：
        // 1. MessageChatMemoryAdvisor — 短期记忆，注入最近 N 条消息到 Prompt
        // 2. VectorStoreChatMemoryAdvisor — 长期记忆，按语义相似度检索历史对话注入 system prompt
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        VectorStoreChatMemoryAdvisor.builder(vectorStore)
                                .defaultTopK(10)
                                .build()
                )
                .build();
    }

    /**
     * 带长期记忆的多轮对话
     * <p>
     * 同一个 conversationId 的请求共享短期上下文，同时长期记忆会按语义相似度
     * 从向量库中检索该 conversationId 的历史对话片段，补充到 system prompt 中。
     * </p>
     *
     * @param conversationId 会话 ID
     * @param userMessage    用户输入
     * @return AI 回复内容
     */
    public String chat(String conversationId, String userMessage) {
        log.info("长期记忆对话，conversationId={}, message={}", conversationId, userMessage);
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    /**
     * 清除指定会话的短期记忆
     * <p>
     * 注意：此方法只清除 JDBC 中的短期记忆，向量库中的长期记忆不会被清除。
     * 长期记忆会随时间自然被新记忆覆盖（语义检索只返回最相关的 topK 条）。
     * </p>
     *
     * @param conversationId 会话 ID
     */
    public void clearMemory(String conversationId) {
        chatMemory.clear(conversationId);
        log.info("已清除短期会话记忆，conversationId={}", conversationId);
    }
}
