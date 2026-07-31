package org.hongxi.cloud.sample.ai.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * DashScope 文生图 ImageModel 实现
 * <p>
 * 实现 Spring AI 的 {@link ImageModel} 接口，封装 DashScope 原生异步 API。
 * 提供两种使用方式：
 * <ul>
 *   <li><b>同步模式</b>：{@link #call(ImagePrompt)} — 提交任务并阻塞等待结果，符合 Spring AI 标准</li>
 *   <li><b>异步模式</b>：{@link #submitAsync} + {@link #getTaskStatus} — 提交后立即返回 taskId，前端轮询获取结果</li>
 * </ul>
 * </p>
 *
 * @author javahongxi
 * @see ImageModel
 */
public class DashScopeImageModel implements ImageModel {

    private static final Logger log = LoggerFactory.getLogger(DashScopeImageModel.class);

    private static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final String CREATE_TASK_URL = DASHSCOPE_BASE_URL + "/services/aigc/image-generation/generation";
    private static final String TASK_STATUS_URL = DASHSCOPE_BASE_URL + "/tasks/";

    /** 同步轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MS = 2000;
    /** 同步轮询最大等待时间（毫秒） */
    private static final long MAX_WAIT_MS = 120_000;
    /** 轮询最大连续失败次数 */
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REF =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String defaultModel;

    public DashScopeImageModel(RestTemplate restTemplate, String apiKey, String defaultModel) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
    }

    // ==================== Spring AI ImageModel 接口（同步模式） ====================

    /**
     * 同步调用：提交任务并阻塞等待结果，符合 Spring AI {@link ImageModel} 契约
     */
    @Override
    public ImageResponse call(ImagePrompt imagePrompt) {
        String prompt = extractPrompt(imagePrompt);
        ImageOptions options = imagePrompt.getOptions();

        String model = (options != null && options.getModel() != null) ? options.getModel() : defaultModel;
        int n = (options != null && options.getN() != null) ? options.getN() : 1;
        String size = resolveSize(options);

        log.info("DashScopeImageModel 同步生成, model: {}, prompt: {}, n: {}, size: {}", model, prompt, n, size);

        String taskId = submitTask(model, prompt, n, size);
        log.info("任务已提交, taskId: {}", taskId);

        List<ImageGeneration> generations = pollUntilComplete(taskId);
        log.info("生成完成, 共 {} 张", generations.size());

        return new ImageResponse(generations, new ImageResponseMetadata(System.currentTimeMillis()));
    }

    // ==================== 异步模式（submit + poll） ====================

    /**
     * 异步提交：仅提交任务，立即返回 taskId
     *
     * @param prompt 文本描述
     * @param n      生成数量
     * @param size   尺寸（如 "1024*1024"）
     * @return DashScope 任务 ID
     */
    public String submitAsync(String prompt, int n, String size) {
        String model = defaultModel;
        log.info("异步提交, model: {}, prompt: {}, n: {}, size: {}", model, prompt, n, size);
        String taskId = submitTask(model, prompt, n, size);
        log.info("任务已提交, taskId: {}", taskId);
        return taskId;
    }

    /**
     * 查询任务状态（单次查询，不阻塞）
     *
     * @param taskId DashScope 任务 ID
     * @return 任务状态（PENDING / RUNNING / SUCCEEDED / FAILED / CANCELED）和图片 URL 列表
     */
    public TaskStatus getTaskStatus(String taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TASK_STATUS_URL + taskId, HttpMethod.GET, request, MAP_TYPE_REF);

            Map<String, Object> output = getOutput(response.getBody());
            String status = (String) output.get("task_status");

            List<String> urls = "SUCCEEDED".equals(status) ? extractUrls(output) : Collections.emptyList();
            return new TaskStatus(taskId, status, urls);
        } catch (Exception e) {
            log.warn("查询任务 {} 状态失败: {}", taskId, e.getMessage());
            return new TaskStatus(taskId, "UNKNOWN", Collections.emptyList());
        }
    }

    // ==================== 内部：同步轮询 ====================

    private List<ImageGeneration> pollUntilComplete(String taskId) {
        long startTime = System.currentTimeMillis();
        int consecutiveFailures = 0;

        while (System.currentTimeMillis() - startTime < MAX_WAIT_MS) {
            TaskStatus status = getTaskStatus(taskId);
            consecutiveFailures = "UNKNOWN".equals(status.status()) ? consecutiveFailures + 1 : 0;

            switch (status.status()) {
                case "SUCCEEDED":
                    return status.urls().stream()
                            .map(url -> new ImageGeneration(new Image(url, null)))
                            .toList();
                case "FAILED":
                case "CANCELED":
                    throw new RuntimeException("任务 " + status.status() + ", taskId: " + taskId);
                case "PENDING":
                case "RUNNING":
                    log.debug("任务 {} 状态: {}, 等待中...", taskId, status.status());
                    break;
                default:
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        throw new RuntimeException("轮询任务连续失败 " + consecutiveFailures + " 次, taskId: " + taskId);
                    }
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("轮询被中断", e);
            }
        }
        throw new RuntimeException("任务超时，taskId: " + taskId);
    }

    // ==================== 内部：DashScope API 交互 ====================

    private String submitTask(String model, String prompt, int n, String size) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("X-DashScope-Async", "enable");

        Map<String, Object> contentItem = new LinkedHashMap<>();
        contentItem.put("text", prompt);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", List.of(contentItem));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", Map.of("messages", List.of(message)));
        body.put("parameters", Map.of("size", size, "n", n, "watermark", false));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                CREATE_TASK_URL, HttpMethod.POST, request, MAP_TYPE_REF);

        Map<String, Object> output = getOutput(response.getBody());
        String taskId = (String) output.get("task_id");
        if (taskId == null) {
            throw new RuntimeException("创建任务失败，未返回 task_id: " + response.getBody());
        }
        return taskId;
    }

    // ==================== 内部：参数提取 & 响应解析 ====================

    private String extractPrompt(ImagePrompt imagePrompt) {
        List<ImageMessage> messages = imagePrompt.getInstructions();
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("ImagePrompt 不能为空");
        }
        return messages.get(0).getText();
    }

    private String resolveSize(ImageOptions options) {
        if (options == null) return "1024*1024";
        Integer width = options.getWidth();
        Integer height = options.getHeight();
        if (width != null && height != null) {
            return width + "*" + height;
        }
        return "1024*1024";
    }

    @SuppressWarnings("unchecked")
    private List<String> extractUrls(Map<String, Object> output) {
        // wan2.7 响应格式：output.choices[].message.content[].image
        List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
        if (choices == null) {
            // 兼容旧版 wanx2.1 格式：output.results[].url
            List<Map<String, String>> results = (List<Map<String, String>>) output.get("results");
            if (results == null) return Collections.emptyList();
            return results.stream().map(r -> r.get("url")).filter(Objects::nonNull).toList();
        }
        return choices.stream()
                .map(choice -> {
                    Map<String, Object> msg = (Map<String, Object>) choice.get("message");
                    if (msg == null) return null;
                    List<Map<String, String>> contentList = (List<Map<String, String>>) msg.get("content");
                    if (contentList == null) return null;
                    return contentList.stream()
                            .filter(c -> "image".equals(c.get("type")) || c.containsKey("image"))
                            .map(c -> c.get("image"))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOutput(Map<String, Object> body) {
        if (body == null) throw new RuntimeException("响应为空");
        Map<String, Object> output = (Map<String, Object>) body.get("output");
        if (output == null) {
            String code = (String) body.get("code");
            String message = (String) body.get("message");
            throw new RuntimeException("DashScope 错误: " + code + " - " + message);
        }
        return output;
    }

    // ==================== 任务状态 DTO ====================

    /**
     * DashScope 任务状态
     *
     * @param taskId 任务 ID
     * @param status 状态：PENDING / RUNNING / SUCCEEDED / FAILED / CANCELED
     * @param urls   图片 URL 列表（仅 SUCCEEDED 时非空）
     */
    public record TaskStatus(String taskId, String status, List<String> urls) {}
}
