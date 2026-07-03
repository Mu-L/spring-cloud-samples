package org.hongxi.cloud.sample.ai.vo;

import java.util.Map;

/**
 * 自定义模板请求
 *
 * @param template  提示词模板（使用 {key} 占位符）
 * @param variables 模板变量键值对
 */
public record CustomTemplateRequest(String template, Map<String, Object> variables) {
}
