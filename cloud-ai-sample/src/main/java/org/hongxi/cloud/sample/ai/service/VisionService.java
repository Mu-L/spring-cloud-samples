package org.hongxi.cloud.sample.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 多模态图像处理服务
 * <p>
 * 演示如何使用 AI 模型理解和描述图片内容。
 * 注意：请将 model 改为支持多模态的模型，如 qwen3.7-plus
 * </p>
 *
 * @author hongxi
 */
@Service
public class VisionService {

    private static final Logger log = LoggerFactory.getLogger(VisionService.class);

    private final ChatClient chatClient;

    public VisionService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 通过 URL 分析图片
     *
     * @param imageUrl 图片 URL
     * @param prompt   提示词
     * @return 图片描述
     */
    public Map<String, Object> analyzeImageByUrl(String imageUrl, String prompt) {
        log.info("分析图片 URL: {}", imageUrl);

        try {
            Resource imageResource = new UrlResource(imageUrl);

            String description = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(prompt)
                            .media(MediaType.IMAGE_JPEG, imageResource))
                    .call()
                    .content();

            log.info("图片描述: {}", description);

            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl", imageUrl);
            result.put("description", description);
            return result;
        } catch (Exception e) {
            log.error("分析图片失败", e);
            throw new RuntimeException("分析图片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传并分析图片
     *
     * @param file   图片文件
     * @param prompt 提示词
     * @return 图片描述
     */
    public Map<String, Object> analyzeUploadedImage(MultipartFile file, String prompt) {
        log.info("上传并分析图片: {}", file.getOriginalFilename());

        try {
            Path tempFile = Files.createTempFile("upload-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);

            Resource imageResource = new UrlResource(tempFile.toUri());

            String description = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(prompt)
                            .media(MediaType.IMAGE_JPEG, imageResource))
                    .call()
                    .content();

            Files.deleteIfExists(tempFile);

            log.info("图片描述: {}", description);

            Map<String, Object> result = new HashMap<>();
            result.put("filename", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("description", description);
            return result;
        } catch (IOException e) {
            log.error("处理上传文件失败", e);
            throw new RuntimeException("处理上传文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * OCR 文字识别
     *
     * @param imageUrl 图片 URL
     * @return 识别的文字
     */
    public Map<String, Object> ocrTextRecognition(String imageUrl) {
        log.info("OCR 文字识别: {}", imageUrl);

        try {
            Resource imageResource = new UrlResource(imageUrl);

            String text = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text("请提取图片中的所有文字，保持原有格式")
                            .media(MediaType.IMAGE_JPEG, imageResource))
                    .call()
                    .content();

            log.info("识别结果: {}", text);

            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl", imageUrl);
            result.put("recognizedText", text);
            return result;
        } catch (Exception e) {
            log.error("OCR 识别失败", e);
            throw new RuntimeException("OCR 识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 图表分析
     *
     * @param imageUrl 图表 URL
     * @return 图表分析结果
     */
    public Map<String, Object> analyzeChart(String imageUrl) {
        log.info("分析图表: {}", imageUrl);

        try {
            Resource imageResource = new UrlResource(imageUrl);

            String analysis = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text("请分析这个图表，包括：\n" +
                                  "1. 图表类型\n" +
                                  "2. 数据趋势\n" +
                                  "3. 关键发现\n" +
                                  "4. 结论建议")
                            .media(MediaType.IMAGE_JPEG, imageResource))
                    .call()
                    .content();

            log.info("图表分析: {}", analysis);

            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl", imageUrl);
            result.put("analysis", analysis);
            return result;
        } catch (Exception e) {
            log.error("图表分析失败", e);
            throw new RuntimeException("图表分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 代码截图转代码
     *
     * @param imageUrl 代码截图 URL
     * @return 转换后的代码
     */
    public Map<String, Object> codeFromImage(String imageUrl) {
        log.info("代码截图转换: {}", imageUrl);

        try {
            Resource imageResource = new UrlResource(imageUrl);

            String code = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text("请将这张图片中的代码完整提取出来，保持格式和缩进")
                            .media(MediaType.IMAGE_JPEG, imageResource))
                    .call()
                    .content();

            log.info("提取的代码: {}", code);

            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl", imageUrl);
            result.put("code", code);
            return result;
        } catch (Exception e) {
            log.error("代码提取失败", e);
            throw new RuntimeException("代码提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 多图片对比分析
     *
     * @param imageUrl1 第一张图片 URL
     * @param imageUrl2 第二张图片 URL
     * @return 对比分析结果
     */
    public Map<String, Object> compareImages(String imageUrl1, String imageUrl2) {
        log.info("对比图片: {} vs {}", imageUrl1, imageUrl2);

        try {
            Resource image1 = new UrlResource(imageUrl1);
            Resource image2 = new UrlResource(imageUrl2);

            String comparison = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text("请对比这两张图片，分析它们的相似点和不同点")
                            .media(MediaType.IMAGE_JPEG, image1)
                            .media(MediaType.IMAGE_JPEG, image2))
                    .call()
                    .content();

            log.info("对比结果: {}", comparison);

            Map<String, Object> result = new HashMap<>();
            result.put("image1", imageUrl1);
            result.put("image2", imageUrl2);
            result.put("comparison", comparison);
            return result;
        } catch (Exception e) {
            log.error("图片对比失败", e);
            throw new RuntimeException("图片对比失败: " + e.getMessage(), e);
        }
    }
}
