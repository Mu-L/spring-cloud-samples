package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.advisor.ToolCallObservationAdvisor;
import org.hongxi.cloud.sample.ai.tool.HttpRequestTool;
import org.hongxi.cloud.sample.ai.tool.TimeTool;
import org.hongxi.cloud.sample.ai.tool.WebSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
     * - "帮我请求 https://jsonplaceholder.typicode.com/posts/1 看看返回什么"
     * </p>
     *
     * @param message 用户消息
     * @return Agent 的回答（SSE 流式输出）
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentChat(@RequestParam String message) {
        log.info("Agent 收到问题: {}", message);
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .tools(timeTool, httpRequestTool, webSearchTool)
                .stream()
                .content()
                .doOnComplete(() -> log.info("Agent 回复完成"))
                .onErrorResume(e -> {
                    // web_search 返回的新闻内容触发了过滤规则，这是模型提供商（阿里云）的内容安全过滤导致的 400 错误
                    log.error("Agent 调用失败: {}", e.getMessage(), e);
                    return Flux.just("抱歉，处理您的问题时出错：" + e.getMessage());
                });
    }

    /**
     * Advisor 链演示
     *
     * @param message 用户消息
     * @return Agent 的回答（SSE 流式输出）
     */
    @GetMapping(value = "/chat-with-advisor", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithAdvisorChain(@RequestParam String message) {
        log.info("Advisor 链演示 - 收到问题: {}", message);
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .tools(timeTool, httpRequestTool, webSearchTool)
                .advisors(new ToolCallObservationAdvisor())
                .stream()
                .content()
                .doOnComplete(() -> log.info("Advisor 链演示 - 完成"))
                .onErrorResume(e -> {
                    // web_search 返回的新闻内容触发了过滤规则，这是模型提供商（阿里云）的内容安全过滤导致的 400 错误
                    log.error("Advisor 链调用失败: {}", e.getMessage(), e);
                    return Flux.just("抱歉，处理您的问题时出错：" + e.getMessage());
                });
    }

    private static final String SYSTEM_PROMPT = """
                            你是一个智能助手，必须通过调用工具来获取信息，禁止凭记忆直接回答。
                            
                            你可以使用的工具包括：
                            - 时间查询：获取当前日期、时间，计算日期差
                            - Web 搜索：搜索实时信息、最新新闻
                            - HTTP 请求：调用 REST API、测试接口
                            
                            回答要求：
                            1. 对于时间问题，必须调用时间工具获取实时数据
                            2. 对于需要最新信息的问题，使用 Web 搜索工具
                            3. 基于工具返回的结果给出完整、有用的回答
                            """;
}
