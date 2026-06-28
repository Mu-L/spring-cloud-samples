package org.hongxi.cloud.sample.ai.vo;

/**
 * AI 通用响应
 *
 * @param message  用户消息
 * @param response AI 回复
 * @author hongxi
 */
public record AiResponse(String message, String response) {
}
