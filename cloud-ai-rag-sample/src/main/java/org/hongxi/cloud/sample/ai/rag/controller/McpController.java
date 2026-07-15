package org.hongxi.cloud.sample.ai.rag.controller;

import org.hongxi.cloud.sample.ai.rag.service.McpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 工具调用控制器
 * <p>
 * 演示从 RAG 模块调用 ai-sample 的 MCP Server 工具：
 * GET /ai/mcp/chat — 通过 MCP 协议调用远程工具（天气、时间等）
 * </p>
 * <p>
 * 调用链路：
 * 客户端 → ai-rag-sample（MCP Client） → ai-sample（MCP Server） → @Tool 方法
 * </p>
 *
 * @author javahongxi
 */
@RestController
@RequestMapping("/ai/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * 通过 MCP 工具进行对话
     * <p>
     * 示例请求：
     * <ul>
     *   <li>GET /ai/mcp/chat?message=北京今天天气怎么样</li>
     *   <li>GET /ai/mcp/chat?message=现在几点了</li>
     *   <li>GET /ai/mcp/chat?message=距离2026-12-31还有多少天</li>
     * </ul>
     * 以上请求会自动触发 ai-sample MCP Server 上的工具调用。
     * </p>
     *
     * @param message 用户问题
     * @return LLM 结合 MCP 工具结果的回答
     */
    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam String message) {
        log.info("MCP 对话请求，message={}", message);
        String answer = mcpService.chat(message);
        return ResponseEntity.ok(answer);
    }
}
