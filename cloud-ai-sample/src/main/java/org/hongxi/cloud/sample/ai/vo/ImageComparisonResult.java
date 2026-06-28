package org.hongxi.cloud.sample.ai.vo;

import java.util.List;

/**
 * 图片对比结果
 *
 * @param imageUrls 对比的图片 URL 列表
 * @param response  AI 对比分析
 * @author hongxi
 */
public record ImageComparisonResult(List<String> imageUrls, String response) {
}
