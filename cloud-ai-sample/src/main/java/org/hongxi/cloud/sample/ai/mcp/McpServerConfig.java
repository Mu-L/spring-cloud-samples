package org.hongxi.cloud.sample.ai.mcp;

import org.hongxi.cloud.sample.ai.tool.HttpRequestTool;
import org.hongxi.cloud.sample.ai.tool.ProjectDemoTool;
import org.hongxi.cloud.sample.ai.tool.TimeTool;
import org.hongxi.cloud.sample.ai.tool.WebSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 配置类
 * <p>
 * 通过 MethodToolCallbackProvider 将 @Tool 标注的服务方法注册到 MCP Server，
 * 使其可被 MCP Client 发现和调用。
 * </p>
 * <p>
 * 这是 Spring AI MCP 的核心配置方式（自 1.0.0 起支持）：
 * 1. 使用 @Tool 注解标注工具方法（统一放在 tool 包下）
 * 2. 使用 MethodToolCallbackProvider 将工具注册到 MCP Server
 * 3. MCP Client 通过 /mcp 端点（Streamable HTTP）自动发现并调用这些工具
 * </p>
 *
 * @author javahongxi
 */
@Configuration
public class McpServerConfig {

    /**
     * 将实用工具统一注册到 MCP Server
     * <p>
     * 复用 tool 包下的工具类，同时用于内部 Tool Calling 和 MCP 对外暴露。
     * </p>
     */
    @Bean
    public ToolCallbackProvider mcpToolProvider(
            TimeTool timeTool,
            HttpRequestTool httpRequestTool,
            WebSearchTool webSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(timeTool, httpRequestTool, webSearchTool)
                .build();
    }
}
