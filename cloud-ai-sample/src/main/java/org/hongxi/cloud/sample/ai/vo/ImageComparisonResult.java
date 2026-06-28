package org.hongxi.cloud.sample.ai.vo;

/**
 * 图片对比结果
 *
 * @param image1     第一张图片 URL
 * @param image2     第二张图片 URL
 * @param comparison 对比分析
 * @author hongxi
 */
public record ImageComparisonResult(String image1, String image2, String comparison) {
}
