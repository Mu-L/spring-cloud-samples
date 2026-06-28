package org.hongxi.cloud.sample.ai.vo;

/**
 * 视觉分析结果
 *
 * @param imageUrl   图片 URL
 * @param response   AI 分析结果
 * @author hongxi
 */
public record VisionResult(String imageUrl, String response) {
}
