package org.hongxi.cloud.sample.ai.service;

import org.hongxi.cloud.sample.ai.support.ReasoningSse;
import org.hongxi.cloud.sample.ai.tool.HttpRequestTool;
import org.hongxi.cloud.sample.ai.tool.TimeTool;
import org.hongxi.cloud.sample.ai.tool.WebSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Tool Calling（工具调用）服务
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
@Service
public class ToolCallingService {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingService.class);

    private final ChatClient chatClient;
    private final TimeTool timeTool;
    private final HttpRequestTool httpRequestTool;
    private final WebSearchTool webSearchTool;

    public ToolCallingService(
            ChatClient.Builder builder,
            TimeTool timeTool,
            HttpRequestTool httpRequestTool,
            WebSearchTool webSearchTool) {
        this.chatClient = builder.build();
        this.timeTool = timeTool;
        this.httpRequestTool = httpRequestTool;
        this.webSearchTool = webSearchTool;
    }

    /**
     * 时间查询 - AI 自动调用时间工具
     * <p>
     * 测试示例: "现在几点了？" / "今天星期几？" / "距离国庆节还有多少天？"
     * </p>
     *
     * @param message 用户问题
     * @return AI 回复（SSE 事件流，含思考内容）
     */
    public Flux<ServerSentEvent<String>> getTime(String message) {
        log.info("时间查询: {}", message);

        return ReasoningSse.toSse(chatClient.prompt()
                .user(message)
                .tools(timeTool)
                .stream()
                .chatResponse()
                .doOnComplete(() -> log.info("AI 回复完成")));
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
     * @return AI 回复（SSE 事件流，含思考内容）
     */
    public Flux<ServerSentEvent<String>> smartAssistant(String message) {
        log.info("智能助手收到问题: {}", message);

        return ReasoningSse.toSse(chatClient.prompt()
                .system("你是一个智能助手，可以根据用户的问题自动调用合适的工具来获取信息。请用中文回答。")
                .user(message)
                .tools(timeTool, httpRequestTool, webSearchTool)
                .stream()
                .chatResponse()
                .doOnComplete(() -> log.info("AI 回复完成")));
    }
}
