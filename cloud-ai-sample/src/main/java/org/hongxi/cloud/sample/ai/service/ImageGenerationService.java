package org.hongxi.cloud.sample.ai.service;

import org.hongxi.cloud.sample.ai.model.DashScopeImageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 图片生成服务
 * <p>
 * 基于自定义 {@link DashScopeImageModel} 的异步模式（submit + poll），
 * 提交任务后立即返回 taskId，前端通过轮询状态接口获取结果。
 * </p>
 *
 * @author javahongxi
 * @see DashScopeImageModel
 */
@Service
public class ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);

    private final DashScopeImageModel dashScopeImageModel;

    public ImageGenerationService(DashScopeImageModel dashScopeImageModel) {
        this.dashScopeImageModel = dashScopeImageModel;
    }

    /**
     * 提交图片生成任务（异步，立即返回）
     *
     * @param prompt 文本描述
     * @param n      生成数量（1~4）
     * @param size   图片尺寸（如 1024x1024 或 1024*1024）
     * @return DashScope 任务 ID
     */
    public String submitTask(String prompt, Integer n, String size) {
        int imageCount = (n != null) ? n : 1;
        String dashScopeSize = parseSize(size);

        log.info("提交图片生成任务, prompt: {}, n: {}, size: {}", prompt, imageCount, dashScopeSize);
        return dashScopeImageModel.submitAsync(prompt, imageCount, dashScopeSize);
    }

    /**
     * 查询任务状态
     *
     * @param taskId DashScope 任务 ID
     * @return 任务状态（包含 status 和 urls）
     */
    public DashScopeImageModel.TaskStatus getTaskStatus(String taskId) {
        return dashScopeImageModel.getTaskStatus(taskId);
    }

    /**
     * 解析尺寸字符串，将 "1024x1024" 转为 DashScope 的 "1024*1024" 格式
     */
    private String parseSize(String size) {
        if (size == null || size.isEmpty()) return "1024*1024";
        return size.replace("x", "*");
    }
}
