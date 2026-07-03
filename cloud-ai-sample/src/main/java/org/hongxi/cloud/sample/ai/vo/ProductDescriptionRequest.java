package org.hongxi.cloud.sample.ai.vo;

/**
 * 产品描述生成请求
 *
 * @param product  产品名称
 * @param category 产品类别
 * @param tone     文案风格（如"专业"、"幽默"、"简洁"）
 */
public record ProductDescriptionRequest(String product, String category, String tone) {
}
