package org.hongxi.cloud.sample.ai.vo;

/**
 * 上传图片分析结果
 *
 * @param filename    文件名
 * @param size        文件大小
 * @param description 图片描述
 * @author hongxi
 */
public record UploadImageAnalysisResult(String filename, Long size, String description) {
}
