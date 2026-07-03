package org.hongxi.cloud.sample.ai.vo;

/**
 * 多轮对话请求
 *
 * @param conversationId 会话 ID（相同 ID 共享对话上下文）
 * @param message        用户输入消息
 */
public record ChatRequest(String conversationId, String message) {
}
