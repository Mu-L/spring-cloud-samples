package org.hongxi.cloud.sample.ai.rag.service;

import org.hongxi.cloud.sample.ai.rag.condition.ConditionalOnMcpClientEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * MCP 工具调用服务
 * <p>
 * 演示通过 MCP Client 调用 ai-sample 模块暴露的 MCP Server 工具。
 * ai-sample 通过 @Tool 注解注册了天气查询、时间查询等工具，
 * 并通过 spring-ai-starter-mcp-server-webmvc 以 SSE 方式对外暴露。
 * <p>
 * 本模块通过 spring-ai-starter-mcp-client 自动发现并连接 MCP Server，
 * ChatClient 在对话时会自动判断是否需要调用远程工具（如天气、时间），
 * 实现跨模块的 AI Agent 工具调用链路。
 * </p>
 *
 * @author javahongxi
 */
@ConditionalOnMcpClientEnabled
@Service
public class McpService {

    private static final Logger log = LoggerFactory.getLogger(McpService.class);

    private final ChatClient mcpChatClient;

    public McpService(@Qualifier("mcpChatClient") ChatClient mcpChatClient) {
        this.mcpChatClient = mcpChatClient;
    }

    /**
     * 通过 MCP 工具进行对话
     * <p>
     * ChatClient 会自动识别用户问题中需要调用的 MCP 工具（如天气、时间），
     * 向 ai-sample 的 MCP Server 发起远程工具调用，并将结果返回给 LLM 生成最终回答。
     * </p>
     *
     * @param message 用户问题，例如："北京今天天气怎么样？"、"现在几点了？"
     * @return LLM 结合 MCP 工具返回结果生成的回答
     */
    public String chat(String message) {
        log.info("MCP 工具调用请求，message={}", message);
        String response = mcpChatClient.prompt()
                .user(message)
                .call()
                .content();
        log.info("MCP 工具调用完成，message={}", message);
        return response;
    }
}
