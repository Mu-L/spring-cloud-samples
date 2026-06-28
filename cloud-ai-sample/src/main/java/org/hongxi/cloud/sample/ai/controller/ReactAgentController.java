package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.tool.SearchTools;
import org.hongxi.cloud.sample.ai.tool.TimeTools;
import org.hongxi.cloud.sample.ai.tool.WeatherTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * ReAct Agent 控制器
 * <p>
 * ReAct (Reasoning + Acting) 是一种结合推理和行动的 Agent 模式。
 * Agent 会根据任务需求，自主决定调用哪些工具来获取信息或执行操作。
 * </p>
 *
 * @author hongxi
 */
@RestController
@RequestMapping("/ai/agent")
public class ReactAgentController {

    private static final Logger log = LoggerFactory.getLogger(ReactAgentController.class);

    private final ChatClient chatClient;
    private final WeatherTools weatherTools;
    private final TimeTools timeTools;
    private final SearchTools searchTools;

    public ReactAgentController(ChatClient.Builder builder,
                                WeatherTools weatherTools,
                                TimeTools timeTools,
                                SearchTools searchTools) {
        this.chatClient = builder.build();
        this.weatherTools = weatherTools;
        this.timeTools = timeTools;
        this.searchTools = searchTools;
    }

    /**
     * ReAct Agent 智能问答
     * <p>
     * Agent 会自动判断需要调用哪些工具来回答问题，并可以进行多步推理。
     * </p>
     * <p>
     * 测试示例：
     * - "北京今天的天气怎么样？适合出门吗？"
     * - "什么是 Apache Dubbo？它的最新版本支持什么协议？"
     * - "现在是几号？距离春节还有多少天？"
     * - "我想了解 Spring AI 的最新发展趋势"
     * </p>
     *
     * @param message 用户消息
     * @return Agent 的回答
     */
    @GetMapping("/chat")
    public String agentChat(@RequestParam String message) {
        log.info("Agent 收到问题: {}", message);
        String response = chatClient.prompt()
                .system("""
                        你是一个智能助手，可以使用各种工具来帮助用户解决问题。
                        
                        你可以使用的工具包括：
                        - 天气查询：获取城市当前天气和天气预报
                        - 时间查询：获取当前日期、时间，计算日期差
                        - 知识搜索：搜索技术主题的相关信息
                        - 最新资讯：获取技术领域的最新动态
                        
                        回答要求：
                        1. 根据问题需要，主动调用合适的工具获取信息
                        2. 基于工具返回的结果给出完整、有用的回答
                        3. 如果一个问题需要多个工具配合，依次调用
                        4. 保持回答简洁、准确、有用
                        """)
                .user(message)
                .tools(weatherTools, timeTools, searchTools)
                .call()
                .content();
        log.info("Agent 回复: {}", response);
        return response;
    }

    /**
     * 复杂任务处理
     * <p>
     * 展示 Agent 如何处理需要多步推理和多个工具调用的复杂任务。
     * </p>
     * <p>
     * 测试示例：
     * - "我想去杭州旅游，帮我查一下杭州的天气，以及介绍一下杭州的著名景点"
     * - "现在是几月？Spring AI 有什么新特性？帮我规划一个学习计划"
     * </p>
     *
     * @param message 任务描述
     * @return 任务执行结果
     */
    @GetMapping("/complex-task")
    public String handleComplexTask(@RequestParam String message) {
        log.info("Agent 收到复杂任务: {}", message);
        String response = chatClient.prompt()
                .system("""
                        你是一个强大的 AI Agent，擅长解决复杂问题。
                        
                        解决复杂问题的步骤：
                        1. 理解任务目标
                        2. 分解任务为多个子任务
                        3. 对每个子任务选择合适的工具获取信息
                        4. 整合所有信息给出最终答案
                        
                        可用的工具：天气查询、时间查询、知识搜索、最新资讯
                        
                        请详细展示你的思考过程和每一步的操作结果。
                        """)
                .user(message)
                .tools(weatherTools, timeTools, searchTools)
                .call()
                .content();
        log.info("Agent 完成复杂任务");
        return response;
    }
}
