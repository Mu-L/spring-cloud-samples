package org.hongxi.cloud.sample.ai.mcp;

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
     * 注册天气工具
     * <p>
     * 将 WeatherToolService 中所有 @Tool 方法注册到 MCP Server
     * </p>
     */
    @Bean
    public ToolCallbackProvider weatherTools(WeatherToolService weatherToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherToolService)
                .build();
    }

    /**
     * 注册系统工具
     * <p>
     * 将 SystemToolService 中所有 @Tool 方法注册到 MCP Server
     * </p>
     */
    @Bean
    public ToolCallbackProvider systemTools(SystemToolService systemToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(systemToolService)
                .build();
    }

    /**
     * 注册数据转换工具
     * <p>
     * 将 ConversionToolService 中所有 @Tool 方法注册到 MCP Server
     * </p>
     */
    @Bean
    public ToolCallbackProvider conversionTools(ConversionToolService conversionToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(conversionToolService)
                .build();
    }
}
