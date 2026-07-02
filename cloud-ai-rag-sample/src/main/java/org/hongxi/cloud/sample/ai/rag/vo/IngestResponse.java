package org.hongxi.cloud.sample.ai.rag.vo;

/**
 * 文档摄入响应
 *
 * @param source  来源标识
 * @param chunks  分块后存储的文档数量
 * @param message 操作结果消息
 */
public record IngestResponse(String source, int chunks, String message) {
}
