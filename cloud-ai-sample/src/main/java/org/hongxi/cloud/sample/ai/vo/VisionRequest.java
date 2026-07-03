package org.hongxi.cloud.sample.ai.vo;

/**
 * 图像分析请求
 *
 * @param imageUrl 图片 URL
 * @param prompt   提示词
 */
public record VisionRequest(String imageUrl, String prompt) {
}
