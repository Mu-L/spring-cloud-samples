package org.hongxi.cloud.sample.ai.vo;

/**
 * 代码解释请求
 *
 * @param code     代码片段
 * @param language 编程语言
 * @param level    解释深度（如"初学者"、"中级"、"高级"）
 */
public record CodeExplainRequest(String code, String language, String level) {
}
