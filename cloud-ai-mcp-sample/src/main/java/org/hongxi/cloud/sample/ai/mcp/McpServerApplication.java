package org.hongxi.cloud.sample.ai.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Server 启动类
 * <p>
 * MCP（Model Context Protocol）是 AI Agent 之间的标准化通信协议。
 * 本应用作为一个 MCP Server，对外暴露 Tool 服务，可以被任何 MCP Client（如 AI 助手、IDE 插件等）调用。
 * </p>
 * <p>
 * 启动后，MCP Server 会在 /mcp 端点提供服务，Client 可以通过 HTTP 调用发现和调用注册的工具。
 * </p>
 *
 * @author hongxi
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
