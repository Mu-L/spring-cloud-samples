package org.hongxi.cloud.sample.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
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

    // 缓存最近的用户消息
    private final List<UserMessage> userMessages = new ArrayList<>();
    // 缓存最近的 AI 回复
    private final List<AssistantMessage> assistantMessages = new ArrayList<>();

    public AdvancedChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 使用 System Message 设定 AI 角色
     *
     * @param message 用户消息
     * @return AI 回复
     */
    @RequestMapping("/system-message")
    public String chatWithSystemMessage(@RequestParam String message) {
        log.info("System Message 对话: {}", message);
        String response = chatClient.prompt()
                .system("你是一个资深的 Java 架构师，擅长设计高并发、高可用的分布式系统。回答要专业、深入。")
                .options(OpenAiChatOptions.builder().temperature(0.4)) // 低温度=更准确、更快回答
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
    @RequestMapping("/few-shot")
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
     * 提取用户信息为结构化数据
     * <p>
     * 测试示例: "我叫张三，今年25岁，是一名软件工程师，喜欢编程和打篮球，邮箱是zhangsan@example.com"
     * </p>
     *
     * @param message 包含用户信息的自然语言文本
     * @return 结构化的用户信息
     */
    @PostMapping("/extract-user")
    public Object extractUserInfo(@RequestParam String message) {
        log.info("提取用户信息: {}", message);

        record UserInfo(String name, Integer age, String email, List<String> hobbies, String occupation) {}

        return chatClient.prompt()
                .system("""
                        你是一个信息提取助手。请从用户的描述中提取个人信息，并以 JSON 格式返回。
                        
                        要求：
                        - name: 姓名（字符串）
                        - age: 年龄（整数）
                        - email: 邮箱（字符串）
                        - hobbies: 爱好（字符串数组）
                        - occupation: 职业（字符串）
                        
                        如果某些信息不存在，用 null 或空数组表示。
                        """)
                .user(message)
                .call()
                .entity(UserInfo.class);
    }

    /**
     * 多轮对话（手动维护上下文）
     *
     * @param message 当前用户消息
     * @return AI 回复
     */
    @RequestMapping("/conversation")
    public String conversation(@RequestParam String message) {
        log.info("多轮对话 - 当前消息: {}", message);

        List<Message> messages = new ArrayList<>();
        messages.addAll(userMessages);
        messages.addAll(assistantMessages);

        String response = chatClient.prompt()
                .messages(messages)
                .user(message)
                .call()
                .content();
        log.info("AI 回复: {}", response);

        if (userMessages.size() > 10) {
            userMessages.remove(0);
            assistantMessages.remove(0);
        } else {
            userMessages.add(UserMessage.builder().text(message).build());
            String summary = response == null ? "" : response.length() > 100 ?
                                                     response.substring(0, 100) + "..." : response;
            assistantMessages.add(AssistantMessage.builder().content(summary).build());
        }

        return response;
    }

    /**
     * 带温度参数的创意性对话
     *
     * @param message 用户消息
     * @return AI 回复
     */
    @RequestMapping("/creative")
    public String creativeChat(@RequestParam String message) {
        log.info("创意性对话: {}", message);
        String response = chatClient.prompt()
                .system("你是一个富有创造力的作家，擅长写故事和诗歌。")
                .options(OpenAiChatOptions.builder().temperature(0.9)) // 高温度=更有创造力
                .user(message)
                .call()
                .content();
        log.info("AI 回复: {}", response);
        return response;
    }
}
