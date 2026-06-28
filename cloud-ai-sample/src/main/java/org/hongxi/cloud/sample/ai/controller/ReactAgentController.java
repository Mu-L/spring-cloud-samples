package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.ReactAgentService;
import org.hongxi.cloud.sample.ai.vo.AgentResult;
import org.hongxi.cloud.sample.ai.vo.TaskResult;
import org.springframework.web.bind.annotation.*;

/**
 * ReAct Agent 控制器
 * <p>
 * ReAct (Reasoning + Acting) 是一种结合推理和行动的 Agent 模式。
 * Agent 会根据任务需求，自主决定调用哪些工具来获取信息或执行操作。
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
@RestController
@RequestMapping("/ai/agent")
public class ReactAgentController {

    private final ReactAgentService reactAgentService;

    public ReactAgentController(ReactAgentService reactAgentService) {
        this.reactAgentService = reactAgentService;
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
    public AgentResult agentChat(@RequestParam String question) {
        return reactAgentService.agentChat(question);
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
    public TaskResult handleComplexTask(@RequestParam String task) {
        return reactAgentService.handleComplexTask(task);
    }
}
