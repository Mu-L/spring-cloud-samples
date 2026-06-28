package org.hongxi.cloud.sample.ai.vo;

/**
 * OCR 文字识别结果
 *
 * @param imageUrl       图片 URL
 * @param recognizedText 识别的文字
 * @author hongxi
 */
public record OcrResult(String imageUrl, String recognizedText) {
}
