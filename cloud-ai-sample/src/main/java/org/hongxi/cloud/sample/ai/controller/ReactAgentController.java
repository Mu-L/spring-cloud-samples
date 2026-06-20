package org.hongxi.cloud.sample.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.hongxi.cloud.sample.ai.tool.SearchTools;
import org.hongxi.cloud.sample.ai.tool.TimeTools;
import org.hongxi.cloud.sample.ai.tool.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ReAct Agent 控制器
 * <p>
 * ReAct (Reasoning + Acting) 是一种结合推理和行动的 Agent 模式。
 * Agent 会根据任务需求，自主决定调用哪些工具来获取信息或执行操作。
 * </p>
 * <p>
 * 这是 Spring AI 2.0 最具价值的特性之一：
 * - 1.x 版本只能通过 Function Calling 实现简单的工具调用
 * - 2.0 版本支持完整的 ReAct Agent 模式，AI 可以进行多步推理和工具调用
 * </p>
 * <p>
 * 工作流程：
 * 1. 接收用户问题
 * 2. 分析问题，判断需要调用哪些工具
 * 3. 选择合适的工具并执行
 * 4. 基于工具返回结果进行推理
 * 5. 如果需要更多信息，继续调用其他工具
 * 6. 生成最终答案
 * </p>
 *
 * @author hongxi
 */
@Slf4j
@RestController
@RequestMapping("/ai/agent")
public class ReactAgentController {

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
     * @param question 用户问题
     * @return Agent 的回答
     */
    @GetMapping("/chat")
    public Map<String, Object> agentChat(@RequestParam String question) {
        log.info("Agent 收到问题: {}", question);

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
                .user(question)
                .tools(weatherTools, timeTools, searchTools)
                .call()
                .content();

        log.info("Agent 回复: {}", response);

        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("answer", response);
        result.put("type", "react-agent");
        return result;
    }

    /**
     * 复杂任务处理
     * <p>
     * 展示 Agent 如何处理需要多步推理和多个工具调用的复杂任务。
     * Agent 会将复杂问题拆解，逐步调用工具获取所需信息，最终整合出完整答案。
     * </p>
     * <p>
     * 测试示例：
     * - "我想去杭州旅游，帮我查一下杭州的天气，以及介绍一下杭州的著名景点"
     * - "现在是几月？Spring AI 有什么新特性？帮我规划一个学习计划"
     * </p>
     *
     * @param task 复杂任务描述
     * @return 任务执行结果
     */
    @GetMapping("/complex-task")
    public Map<String, Object> handleComplexTask(@RequestParam String task) {
        log.info("Agent 收到复杂任务: {}", task);

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
                .user(task)
                .tools(weatherTools, timeTools, searchTools)
                .call()
                .content();

        log.info("Agent 完成复杂任务");

        Map<String, Object> result = new HashMap<>();
        result.put("task", task);
        result.put("solution", response);
        result.put("type", "complex-task-solving");
        return result;
    }
}
