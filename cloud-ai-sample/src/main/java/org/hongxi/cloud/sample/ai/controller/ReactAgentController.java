package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.advisor.ToolCallObservationAdvisor;
import org.hongxi.cloud.sample.ai.tool.HttpRequestTool;
import org.hongxi.cloud.sample.ai.tool.TimeTool;
import org.hongxi.cloud.sample.ai.tool.WebSearchTool;
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
 * <p>
 * Spring AI 2.0 核心架构升级：工具调用循环从 ChatModel 内部的"黑盒"
 * 提升为 Advisor 链中的"一等公民"（ToolCallingAdvisor）。
 * 开发者可以在工具调用前后插入自定义逻辑，实现完整的可观测性。
 * </p>
 *
 * @author javahongxi
 */
@RestController
@RequestMapping("/ai/agent")
public class ReactAgentController {

    private static final Logger log = LoggerFactory.getLogger(ReactAgentController.class);

    private final ChatClient chatClient;
    private final TimeTool timeTool;
    private final HttpRequestTool httpRequestTool;
    private final WebSearchTool webSearchTool;

    public ReactAgentController(ChatClient.Builder builder,
                                TimeTool timeTool,
                                HttpRequestTool httpRequestTool,
                                WebSearchTool webSearchTool) {
        this.chatClient = builder.build();
        this.timeTool = timeTool;
        this.httpRequestTool = httpRequestTool;
        this.webSearchTool = webSearchTool;
    }

    /**
     * ReAct Agent 智能问答
     * <p>
     * Agent 会自动判断需要调用哪些工具来回答问题，并可以进行多步推理。
     * </p>
     * <p>
     * 测试示例：
     * - "现在几点了？"
     * - "搜索一下最近有什么新上映的电影"
     * - "帮我请求 https://httpbin.org/get 看看返回什么"
     * </p>
     *
     * @param message 用户消息
     * @return Agent 的回答
     */
    @RequestMapping("/chat")
    public String agentChat(@RequestParam String message) {
        log.info("Agent 收到问题: {}", message);
        String response = chatClient.prompt()
                .system("""
                        你是一个智能助手，必须通过调用工具来获取信息，禁止凭记忆直接回答。
                        
                        你可以使用的工具包括：
                        - 时间查询：获取当前日期、时间，计算日期差
                        - Web 搜索：搜索实时信息、最新新闻
                        - HTTP 请求：调用 REST API、测试接口
                        
                        回答要求：
                        1. 对于时间问题，必须调用时间工具获取实时数据
                        2. 对于需要最新信息的问题，使用 Web 搜索工具
                        3. 对于需要调用 API 的场景，使用 HTTP 请求工具
                        4. 基于工具返回的结果给出完整、有用的回答
                        5. 如果一个问题需要多个工具配合，依次调用
                        """)
                .user(message)
                .tools(timeTool, httpRequestTool, webSearchTool)
                .call()
                .content();
        log.info("Agent 回复: {}", response);
        return response;
    }

    /**
     * 展示 Advisor 链架构的工具调用
     * <p>
     * 本接口显式演示 Spring AI 2.0 的 Advisor 链机制：
     * <pre>
     * ChatClient → [ToolCallingAdvisor(+300)] → [ToolCallObservationAdvisor(+400)] → ChatModel
     *                ↑ 递归驱动工具调用循环       ↑ 位于 TCA 之后，每次迭代都被观测
     * </pre>
     * </p>
     * <p>
     * 与 {@link #agentChat(String)} 的区别：
     * <ul>
     *   <li>agentChat 使用 .tools() 隐式添加 ToolCallingAdvisor，工具调用过程不可见</li>
     *   <li>本接口通过 .advisors() 显式添加自定义 Advisor，可观测工具调用循环的每次迭代</li>
     * </ul>
     * </p>
     * <p>
     * 测试示例：
     * - "现在几点了？"
     * - "搜索一下 Spring AI 2.0 的最新特性"
     * </p>
     *
     * @param message 用户消息
     * @return Agent 的回答
     */
    @RequestMapping("/chat-with-advisor")
    public String chatWithAdvisorChain(@RequestParam String message) {
        log.info("Advisor 链演示 - 收到问题: {}", message);
        String response = chatClient.prompt()
                .system("""
                        你是一个智能助手，必须通过调用工具来获取信息，禁止凭记忆直接回答。
                        
                        你可以使用的工具包括：
                        - 时间查询：获取当前日期、时间，计算日期差
                        - Web 搜索：搜索实时信息、最新新闻
                        - HTTP 请求：调用 REST API、测试接口
                        
                        回答要求：
                        1. 对于时间问题，必须调用时间工具获取实时数据
                        2. 对于需要最新信息的问题，使用 Web 搜索工具
                        3. 基于工具返回的结果给出完整、有用的回答
                        """)
                .user(message)
                .tools(timeTool, httpRequestTool, webSearchTool)
                // 显式添加自定义 Advisor（order=400，位于 ToolCallingAdvisor 之后）
                // Advisor 链执行顺序（按 order 升序）：
                //   ToolCallingAdvisor(+300) → ToolCallObservationAdvisor(+400) → ChatModel
                // TCA 递归时 chain.copy(this) 只包含 order>300 的 Advisor，
                // 因此 observer 会在每次工具调用迭代中被触发
                .advisors(new ToolCallObservationAdvisor())
                .call()
                .content();
        log.info("Advisor 链演示 - 完成");
        return response;
    }
}
