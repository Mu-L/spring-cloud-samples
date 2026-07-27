package org.hongxi.cloud.sample.ai.rag.vo;

/**
 * 多轮对话响应
 *
 * @param conversationId 会话 ID
 * @param reply          AI 回复内容
 */
public record ChatResponse(String conversationId, String reply) {
}
