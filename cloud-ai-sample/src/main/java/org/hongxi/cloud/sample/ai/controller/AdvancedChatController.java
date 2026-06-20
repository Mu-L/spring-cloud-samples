package org.hongxi.cloud.sample.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.hongxi.cloud.sample.ai.service.AdvancedChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ChatClient 高级用法
 * <p>
 * 演示 System Message、Few-shot Prompting、多轮对话等高级特性
 * </p>
 *
 * @author hongxi
 */
@Slf4j
@RestController
@RequestMapping("/ai/advanced")
public class AdvancedChatController {

    private final AdvancedChatService advancedChatService;

    public AdvancedChatController(AdvancedChatService advancedChatService) {
        this.advancedChatService = advancedChatService;
    }

    /**
     * 使用 System Message 设定 AI 角色
     *
     * @param message 用户消息
     * @return AI 回复
     */
    @PostMapping("/system-message")
    public Map<String, String> chatWithSystemMessage(@RequestParam String message) {
        return advancedChatService.chatWithSystemMessage(message);
    }

    /**
     * Few-shot Prompting - 提供示例引导 AI
     *
     * @param message 用户消息
     * @return AI 回复
     */
    @PostMapping("/few-shot")
    public Map<String, String> fewShotPrompting(@RequestParam String message) {
        return advancedChatService.fewShotPrompting(message);
    }

    /**
     * 多轮对话（手动维护上下文）
     *
     * @param messages 消息历史（交替的用户和 AI 消息）
     * @param currentMessage 当前用户消息
     * @return AI 回复
     */
    @PostMapping("/conversation")
    public Map<String, Object> conversation(
            @RequestBody(required = false) List<Map<String, String>> messages,
            @RequestParam String currentMessage) {
        return advancedChatService.conversation(messages, currentMessage);
    }

    /**
     * 带温度参数的创意性对话
     *
     * @param message 用户消息
     * @return AI 回复
     */
    @PostMapping("/creative")
    public Map<String, String> creativeChat(@RequestParam String message) {
        return advancedChatService.creativeChat(message);
    }
}
