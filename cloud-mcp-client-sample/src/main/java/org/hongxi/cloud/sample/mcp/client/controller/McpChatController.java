package org.hongxi.cloud.sample.mcp.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * MCP Client 聊天控制器
 * <p>
 * 演示通过 MCP Client 连接远程 MCP Server，自动发现并调用其注册的工具。
 * <p>
 * MCP Client 启动时自动连接 MCP Server（cloud-ai-sample），
 * 发现 Server 上注册的工具（get_current_date_time、days_until、http_request、web_search 等），
 * 并通过 {@link ToolCallbackProvider} 注入到 ChatClient 中。
 * <p>
 * 使用示例：
 * <ul>
 *   <li>GET /mcp/chat?message=现在几点了 — AI 会调用 MCP Server 上的时间工具</li>
 *   <li>GET /mcp/chat?message=最近有什么新上映的电影 — AI 会调用搜索工具</li>
 *   <li>GET /mcp/chat/stream?message=距离2026年春节还有多少天 — 流式响应</li>
 * </ul>
 *
 * @author javahongxi
 */
@RestController
@RequestMapping("/mcp")
public class McpChatController {

    private static final Logger log = LoggerFactory.getLogger(McpChatController.class);

    private static final String SYSTEM_PROMPT = """
            你是一个具备实时信息获取能力的智能助手，拥有以下远程工具：
            - get_current_date_time：获取当前真实日期和时间
            - days_until：计算距离目标日期的天数
            - web_search：网络搜索
            - http_request：HTTP 请求
            
            【核心规则 — 时间优先】
            你自身不具备时间感知能力，训练数据有截止日期，因此：
            1. 每次收到用户消息时，第一步必须调用 get_current_date_time 获取当前真实日期。
            2. 拿到真实日期后，再决定是否需要调用其他工具（如 web_search）。
            3. 调用其他工具时，所有涉及时间的参数必须使用第 1 步获取的真实年月日，
               严禁使用训练数据中的年份或自行推测的年份。
            4. 当用户问「最近」「最新」等模糊时间范围的问题时，搜索关键词必须精确到
               当前月份，不要只写年份。例如当前是 2026 年 7 月，就搜 "2026年7月" 而非 "2026年"。
            
            示例：用户问"最近有什么新上映的电影"
            → 先调用 get_current_date_time 得到 2026-07-31
            → 再调用 web_search 时传入 "2026年7月最新上映电影"
            
            【其他规则】
            - 用户询问两个日期之间的天数差时，使用 days_until 工具。
            - 回答简洁准确，优先使用工具返回的真实数据，不要编造。
            """;

    private final ChatClient chatClient;

    /**
     * 构造 ChatClient，注入 MCP Server 提供的工具。
     * <p>
     * {@code mcpToolCallbacks} 由 Spring AI MCP Client 自动配置，
     * 它从远程 MCP Server 发现所有可用工具并转换为 Spring AI 的 ToolCallback。
     *
     * @param chatClientBuilder ChatClient 构建器
     * @param mcpToolCallbacks  MCP Client 自动注册的工具回调（来自远程 MCP Server）
     */
    public McpChatController(ChatClient.Builder chatClientBuilder, ToolCallbackProvider mcpToolCallbacks) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(mcpToolCallbacks)
                .build();
    }

    /**
     * 同步聊天接口 — AI 自动选择并调用 MCP Server 上的工具
     */
    @RequestMapping("/chat")
    public String chat(@RequestParam String message) {
        log.info("MCP Client 收到请求: {}", message);
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 流式聊天接口（SSE）— AI 自动选择并调用 MCP Server 上的工具
     */
    @RequestMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message) {
        log.info("MCP Client 流式请求: {}", message);
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
