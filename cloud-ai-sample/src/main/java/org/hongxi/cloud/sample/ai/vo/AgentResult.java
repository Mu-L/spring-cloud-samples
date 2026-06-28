package org.hongxi.cloud.sample.ai.vo;

/**
 * Agent 问答结果
 *
 * @param question 用户问题
 * @param answer   Agent 回答
 * @param type     类型标识
 * @author hongxi
 */
public record AgentResult(String question, String answer, String type) {
}
