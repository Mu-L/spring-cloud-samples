package org.hongxi.cloud.sample.ai.vo;

/**
 * 工具调用请求
 *
 * @param message 用户问题
 */
public record ToolCallingRequest(String message) {
}
