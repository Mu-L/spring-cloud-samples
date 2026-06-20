package org.hongxi.cloud.sample.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatClient 高级用法服务
 * <p>
 * 提供 System Message、Few-shot Prompting、多轮对话等高级特性
 * </p>
 *
 * @author hongxi
 */
@Slf4j
@Service
public class AdvancedChatService {

    private final ChatClient chatClient;

    public AdvancedChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 使用 System Message 设定 AI 角色
     *
     * @param message 用户消息
     * @return AI 回复
     */
    public Map<String, String> chatWithSystemMessage(String message) {
        log.info("System Message 对话: {}", message);

        String response = chatClient.prompt()
                .system("你是一个资深的 Java 架构师，擅长设计高并发、高可用的分布式系统。回答要专业、深入。")
                .user(message)
                .call()
                .content();

        log.info("AI 回复: {}", response);

        Map<String, String> result = new HashMap<>();
        result.put("userMessage", message);
        result.put("aiResponse", response);
        return result;
    }

    /**
     * Few-shot Prompting - 提供示例引导 AI
     *
     * @param message 用户消息
     * @return AI 回复
     */
    public Map<String, String> fewShotPrompting(String message) {
        log.info("Few-shot 提示: {}", message);

        String response = chatClient.prompt()
                .system("""
                        你是一个代码翻译助手，请将用户的自然语言转换为 Java 代码。
                        
                        示例 1:
                        用户: 创建一个字符串变量 name，值为 "Hello"
                        AI: String name = "Hello";
                        
                        示例 2:
                        用户: 创建一个列表，包含 1, 2, 3
                        AI: List<Integer> list = Arrays.asList(1, 2, 3);
                        
                        现在请处理用户的请求：
                        """)
                .user(message)
                .call()
                .content();

        log.info("AI 回复: {}", response);

        Map<String, String> result = new HashMap<>();
        result.put("userMessage", message);
        result.put("aiResponse", response);
        return result;
    }

    /**
     * 多轮对话（手动维护上下文）
     *
     * @param messages 消息历史（交替的用户和 AI 消息）
     * @param currentMessage 当前用户消息
     * @return AI 回复
     */
    public Map<String, Object> conversation(List<Map<String, String>> messages, String currentMessage) {
        log.info("多轮对话 - 当前消息: {}", currentMessage);

        // 简化测试，使用固定上下文
        List<Message> history = new ArrayList<>();
        history.add(UserMessage.builder().text("你好").build());
        history.add(AssistantMessage.builder().content("你好！有什么可以帮助你的？").build());
        String response = chatClient.prompt()
                .messages(history)
                .user(currentMessage)
                .call()
                .content();

        log.info("AI 回复: {}", response);

        Map<String, Object> result = new HashMap<>();
        result.put("currentMessage", currentMessage);
        result.put("aiResponse", response);
        result.put("messageCount", messages == null ? 1 : messages.size() + 1);
        return result;
    }

    /**
     * 带温度参数的创意性对话
     *
     * @param message 用户消息
     * @return AI 回复
     */
    public Map<String, String> creativeChat(String message) {
        log.info("创意性对话: {}", message);

        String response = chatClient.prompt()
                .system("你是一个富有创造力的作家，擅长写故事和诗歌。")
                .user(message)
                .call()
                .content();

        log.info("AI 回复: {}", response);

        Map<String, String> result = new HashMap<>();
        result.put("userMessage", message);
        result.put("aiResponse", response);
        return result;
    }
}
