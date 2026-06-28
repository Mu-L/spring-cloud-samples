package org.hongxi.cloud.sample.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatClient 高级用法控制器
 * <p>
 * 演示 System Message、Few-shot Prompting、多轮对话等高级特性
 * </p>
 *
 * @author hongxi
 */
@RestController
@RequestMapping("/ai/advanced")
public class AdvancedChatController {

    private static final Logger log = LoggerFactory.getLogger(AdvancedChatController.class);

    private final ChatClient chatClient;

    public AdvancedChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 使用 System Message 设定 AI 角色
     *
     * @param message 用户消息
     * @return AI 回复
     */
    @PostMapping("/system-message")
    public String chatWithSystemMessage(@RequestParam String message) {
        log.info("System Message 对话: {}", message);
        String response = chatClient.prompt()
                .system("你是一个资深的 Java 架构师，擅长设计高并发、高可用的分布式系统。回答要专业、深入。")
                .user(message)
                .call()
                .content();
        log.info("AI 回复: {}", response);
        return response;
    }

    /**
     * Few-shot Prompting - 提供示例引导 AI
     *
     * @param message 用户消息
     * @return AI 回复
     */
    @PostMapping("/few-shot")
    public String fewShotPrompting(@RequestParam String message) {
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
        return response;
    }

    /**
     * 多轮对话（手动维护上下文）
     *
     * @param message 当前用户消息
     * @param history 历史消息（可选）
     * @return AI 回复
     */
    @PostMapping("/conversation")
    public String conversation(
            @RequestParam String message,
            @RequestBody(required = false) List<String> history) {
        log.info("多轮对话 - 当前消息: {}", message);
        List<Message> messages = new ArrayList<>();
        messages.add(UserMessage.builder().text("你好").build());
        messages.add(AssistantMessage.builder().content("你好！有什么可以帮助你的？").build());
        if (history != null) {
            for (String h : history) {
                messages.add(UserMessage.builder().text(h).build());
            }
        }
        String response = chatClient.prompt()
                .messages(messages)
                .user(message)
                .call()
                .content();
        log.info("AI 回复: {}", response);
        return response;
    }

    /**
     * 带温度参数的创意性对话
     *
     * @param message 用户消息
     * @return AI 回复
     */
    @PostMapping("/creative")
    public String creativeChat(@RequestParam String message) {
        log.info("创意性对话: {}", message);
        String response = chatClient.prompt()
                .system("你是一个富有创造力的作家，擅长写故事和诗歌。")
                .user(message)
                .call()
                .content();
        log.info("AI 回复: {}", response);
        return response;
    }
}
