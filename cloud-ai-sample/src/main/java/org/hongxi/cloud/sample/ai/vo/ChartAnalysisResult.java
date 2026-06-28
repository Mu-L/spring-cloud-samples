package org.hongxi.cloud.sample.ai.vo;

/**
 * 图表分析结果
 *
 * @param imageUrl 图表 URL
 * @param analysis 分析内容
 * @author hongxi
 */
public record ChartAnalysisResult(String imageUrl, String analysis) {
}
