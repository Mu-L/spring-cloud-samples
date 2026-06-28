package org.hongxi.cloud.sample.ai.vo;

/**
 * 代码提取结果
 *
 * @param imageUrl 代码截图 URL
 * @param code     提取的代码
 * @author hongxi
 */
public record CodeExtractionResult(String imageUrl, String code) {
}
