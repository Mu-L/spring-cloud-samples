package org.hongxi.cloud.sample.ai.vo;

/**
 * 图片对比请求
 *
 * @param imageUrl1 第一张图片 URL
 * @param imageUrl2 第二张图片 URL
 */
public record ImageCompareRequest(String imageUrl1, String imageUrl2) {
}
