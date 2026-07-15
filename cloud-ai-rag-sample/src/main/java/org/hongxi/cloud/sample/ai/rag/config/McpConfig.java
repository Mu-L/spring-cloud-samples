package org.hongxi.cloud.sample.ai.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Client 配置
 * <p>
 * 从 ai-sample 的 MCP Server 自动发现工具并注册到 ChatClient，
 * 使 RAG 模块具备调用远程 MCP 工具（天气查询、时间查询等）的能力。
 * </p>
 *
 * @author javahongxi
 */
@Configuration
public class McpConfig {

    /**
     * 创建具备 MCP 工具调用能力的 ChatClient
     * <p>
     * ToolCallbackProvider 由 spring-ai-starter-mcp-client 自动配置，
     * 包含从 ai-sample MCP Server 发现的所有远程工具（如天气、时间等）。
     * </p>
     *
     * @param chatClientBuilder    自动注入的 ChatClient.Builder（已绑定 ChatModel）
     * @param toolCallbackProvider MCP 工具回调提供者（自动配置）
     * @return 具备 MCP 工具调用能力的 ChatClient
     */
    @Bean("mcpChatClient")
    public ChatClient mcpChatClient(ChatClient.Builder chatClientBuilder,
                                    ToolCallbackProvider toolCallbackProvider) {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        return chatClientBuilder
                .defaultTools((Object[]) callbacks)
                .build();
    }
}
