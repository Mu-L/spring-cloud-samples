package org.hongxi.cloud.sample.ai.vo;

/**
 * 图片分析结果
 *
 * @param imageUrl    图片 URL
 * @param description 图片描述
 * @author hongxi
 */
public record ImageAnalysisResult(String imageUrl, String description) {
}
