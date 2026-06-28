package org.hongxi.cloud.sample.ai.vo;

/**
 * 问答结果
 *
 * @param question 用户问题
 * @param answer   AI 回答
 * @author hongxi
 */
public record QaResult(String question, String answer) {
}
