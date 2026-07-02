package org.hongxi.cloud.sample.ai.rag.vo;

/**
 * 文档摄入请求
 *
 * @param content 文本内容
 * @param source  来源标识
 */
public record IngestRequest(String content, String source) {
}
