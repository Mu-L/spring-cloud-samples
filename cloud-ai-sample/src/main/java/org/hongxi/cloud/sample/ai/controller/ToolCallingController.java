package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.ToolCallingService;
import org.hongxi.cloud.sample.ai.vo.QaResult;
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
 * @author hongxi
 */
@RestController
@RequestMapping("/ai/tool")
public class ToolCallingController {

    private final ToolCallingService toolCallingService;

    public ToolCallingController(ToolCallingService toolCallingService) {
        this.toolCallingService = toolCallingService;
    }

    /**
     * 天气查询 - AI 自动调用天气工具
     * <p>
     * 测试示例: "北京今天的天气怎么样？"
     * </p>
     *
     * @param question 用户问题
     * @return AI 回复
     */
    @GetMapping("/weather")
    public QaResult getWeather(@RequestParam String question) {
        return toolCallingService.getWeather(question);
    }

    /**
     * 时间查询 - AI 自动调用时间工具
     * <p>
     * 测试示例: "现在几点了？" / "今天星期几？" / "距离国庆节还有多少天？"
     * </p>
     *
     * @param question 用户问题
     * @return AI 回复
     */
    @GetMapping("/time")
    public QaResult getTime(@RequestParam String question) {
        return toolCallingService.getTime(question);
    }

    /**
     * 智能助手 - 自动选择合适的工具
     * <p>
     * AI 会根据问题自动选择调用哪些工具：
     * - "帮我查一下上海的天气" → 调用 WeatherTools
     * - "现在几点了？" → 调用 TimeTools
     * - "什么是 Spring AI？" → 调用 SearchTools
     * </p>
     *
     * @param question 用户问题
     * @return AI 回复
     */
    @GetMapping("/ask")
    public QaResult smartAssistant(@RequestParam String question) {
        return toolCallingService.smartAssistant(question);
    }
}
