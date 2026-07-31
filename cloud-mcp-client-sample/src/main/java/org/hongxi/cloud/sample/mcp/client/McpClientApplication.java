package org.hongxi.cloud.sample.mcp.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Client Sample Application
 * <p>
 * 通过 MCP Client 连接远程 MCP Server（cloud-ai-sample），
 * 自动发现并调用 Server 上注册的工具（如时间查询、HTTP 请求、Web 搜索等）。
 * <p>
 * 启动前需先启动 cloud-ai-sample（MCP Server，端口 8888）。
 * <p>
 *
 * @author javahongxi
 */
@SpringBootApplication
public class McpClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }
}
