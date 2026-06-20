package org.hongxi.cloud.sample.ai.mcp;

import org.hongxi.cloud.sample.ai.tool.WeatherTools;
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
 * 这是 Spring AI 2.0 MCP 的核心配置方式：
 * 1. 使用 @Tool 注解标注工具方法
 * 2. 使用 MethodToolCallbackProvider 将工具注册到 MCP Server
 * 3. MCP Client 通过 /mcp 端点自动发现并调用这些工具
 * </p>
 *
 * @author hongxi
 */
@Configuration
public class McpServerConfig {

    /**
     * 注册天气工具到 MCP Server
     * <p>
     * 复用 tool/WeatherTools，同时用于 Tool Calling 和 MCP 对外暴露
     * </p>
     */
    @Bean
    public ToolCallbackProvider mcpWeatherToolProvider(WeatherTools weatherTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools)
                .build();
    }

    /**
     * 注册系统工具到 MCP Server
     */
    @Bean
    public ToolCallbackProvider mcpSystemToolProvider(SystemToolService systemToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(systemToolService)
                .build();
    }

    /**
     * 注册数据转换工具到 MCP Server
     */
    @Bean
    public ToolCallbackProvider mcpConversionToolProvider(ConversionToolService conversionToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(conversionToolService)
                .build();
    }
}
