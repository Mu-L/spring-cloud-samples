package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.VisionService;
import org.hongxi.cloud.sample.ai.vo.ChartAnalysisResult;
import org.hongxi.cloud.sample.ai.vo.CodeExtractionResult;
import org.hongxi.cloud.sample.ai.vo.ImageAnalysisResult;
import org.hongxi.cloud.sample.ai.vo.ImageComparisonResult;
import org.hongxi.cloud.sample.ai.vo.OcrResult;
import org.hongxi.cloud.sample.ai.vo.UploadImageAnalysisResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 多模态图像处理控制器
 * <p>
 * 演示如何使用 AI 模型理解和描述图片内容。
 * 注意：请将 model 改为支持多模态的模型，如 qwen3.7-plus
 * </p>
 *
 * @author hongxi
 */
@RestController
@RequestMapping("/ai/vision")
public class VisionController {

    private final VisionService visionService;

    public VisionController(VisionService visionService) {
        this.visionService = visionService;
    }

    /**
     * 通过 URL 分析图片
     *
     * @param imageUrl 图片 URL
     * @param prompt   提示词（可选）
     * @return 图片描述
     */
    @PostMapping("/analyze-url")
    public ImageAnalysisResult analyzeImageByUrl(@RequestParam String imageUrl,
                                                  @RequestParam(defaultValue = "请详细描述这张图片的内容") String prompt) {
        return visionService.analyzeImageByUrl(imageUrl, prompt);
    }

    /**
     * 上传并分析图片
     *
     * @param file   图片文件
     * @param prompt 提示词（可选）
     * @return 图片描述
     */
    @PostMapping("/analyze-upload")
    public UploadImageAnalysisResult analyzeUploadedFile(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(defaultValue = "请详细描述这张图片的内容") String prompt) {
        return visionService.analyzeUploadedImage(file, prompt);
    }

    /**
     * OCR 文字识别
     *
     * @param imageUrl 图片 URL
     * @return 识别的文字
     */
    @PostMapping("/ocr")
    public OcrResult ocrTextRecognition(@RequestParam String imageUrl) {
        return visionService.ocrTextRecognition(imageUrl);
    }

    /**
     * 图表分析
     *
     * @param imageUrl 图表 URL
     * @return 图表分析结果
     */
    @PostMapping("/chart-analysis")
    public ChartAnalysisResult analyzeChart(@RequestParam String imageUrl) {
        return visionService.analyzeChart(imageUrl);
    }

    /**
     * 代码截图转代码
     *
     * @param imageUrl 代码截图 URL
     * @return 转换后的代码
     */
    @PostMapping("/code-from-image")
    public CodeExtractionResult codeFromImage(@RequestParam String imageUrl) {
        return visionService.codeFromImage(imageUrl);
    }

    /**
     * 多图片对比分析
     *
     * @param imageUrl1 第一张图片 URL
     * @param imageUrl2 第二张图片 URL
     * @return 对比分析结果
     */
    @PostMapping("/compare")
    public ImageComparisonResult compareImages(@RequestParam String imageUrl1,
                                              @RequestParam String imageUrl2) {
        return visionService.compareImages(imageUrl1, imageUrl2);
    }
}
