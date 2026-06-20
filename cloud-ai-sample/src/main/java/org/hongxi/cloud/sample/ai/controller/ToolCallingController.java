package org.hongxi.cloud.sample.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.hongxi.cloud.sample.ai.tool.TimeTools;
import org.hongxi.cloud.sample.ai.tool.WeatherTools;
import org.hongxi.cloud.sample.ai.tool.SearchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool Calling（工具调用）示例控制器
 * <p>
 * 演示 Spring AI 2.0 的 @Tool 注解如何让 AI 模型自动调用 Java 方法来获取实时数据。
 * 这是 2.0 相比 1.x 的核心区别之一：AI 模型可以根据用户问题自主决定是否调用工具。
 * </p>
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
@Slf4j
@RestController
@RequestMapping("/ai/tool")
public class ToolCallingController {

    private final ChatClient chatClient;
    private final WeatherTools weatherTools;
    private final TimeTools timeTools;
    private final SearchTools searchTools;

    public ToolCallingController(
            ChatClient.Builder builder,
            WeatherTools weatherTools,
            TimeTools timeTools,
            SearchTools searchTools) {
        this.chatClient = builder.build();
        this.weatherTools = weatherTools;
        this.timeTools = timeTools;
        this.searchTools = searchTools;
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
    public Map<String, Object> getWeather(@RequestParam String question) {
        log.info("天气查询: {}", question);

        String response = chatClient.prompt()
                .user(question)
                .tools(weatherTools)
                .call()
                .content();

        log.info("AI 回复: {}", response);

        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("answer", response);
        return result;
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
    public Map<String, Object> getTime(@RequestParam String question) {
        log.info("时间查询: {}", question);

        String response = chatClient.prompt()
                .user(question)
                .tools(timeTools)
                .call()
                .content();

        log.info("AI 回复: {}", response);

        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("answer", response);
        return result;
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
    public Map<String, Object> smartAssistant(@RequestParam String question) {
        log.info("智能助手收到问题: {}", question);

        String response = chatClient.prompt()
                .system("你是一个智能助手，可以根据用户的问题自动调用合适的工具来获取信息。请用中文回答。")
                .user(question)
                .tools(weatherTools, timeTools, searchTools)
                .call()
                .content();

        log.info("AI 回复: {}", response);

        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("answer", response);
        return result;
    }
}
