package org.hongxi.cloud.sample.ai.vo;

/**
 * Agent 响应结果
 *
 * @param message  用户消息
 * @param response AI 回复
 * @param type     Agent 类型标识
 * @author hongxi
 */
public record AgentResult(String message, String response, String type) {
}
