package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.ToolCallingService;
import org.springframework.web.bind.annotation.*;

/**
 * Tool Calling（工具调用）示例控制器
 *
 * <p>
 * 工作流程：
 * 1. 用户发送问题
 * 2. AI 模型分析问题，判断是否需要调用工具
 * 3. 如果需要，AI 自动生成工具调用请求（函数名 + 参数）
 * 4. Spring AI 执行对应的 Java 方法，将结果返回给 AI
 * 5. AI 基于工具返回的结果生成最终回答
 * </p>
 *
 * @author javahongxi
 */
@RestController
@RequestMapping("/ai/tool")
public class ToolCallingController {

    private final ToolCallingService toolCallingService;

    public ToolCallingController(ToolCallingService toolCallingService) {
        this.toolCallingService = toolCallingService;
    }

    /**
     * 时间查询 - AI 自动调用时间工具
     * <p>
     * 测试示例: "现在几点了？" / "今天星期几？" / "距离国庆节还有多少天？"
     * </p>
     *
     * @param message 用户问题
     * @return AI 回复
     */
    @GetMapping("/time")
    public String getTime(@RequestParam String message) {
        return toolCallingService.getTime(message);
    }

    /**
     * 智能助手 - 自动选择合适的工具
     * <p>
     * AI 会根据问题自动选择调用哪些工具：
     * - "现在几点了？" → 调用 TimeTool
     * - "搜索一下最近有什么新上映的电影" → 调用 WebSearchTool
     * - "帮我请求一下 https://jsonplaceholder.typicode.com/posts/1" → 调用 HttpRequestTool
     * </p>
     *
     * @param message 用户问题
     * @return AI 回复
     */
    @GetMapping("/ask")
    public String smartAssistant(@RequestParam String message) {
        return toolCallingService.smartAssistant(message);
    }
}
