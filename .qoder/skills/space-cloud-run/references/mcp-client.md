# Spring AI MCP Client Sample


## 架构

```
┌─────────────────┐     Streamable HTTP      ┌─────────────────┐
│   MCP Client    │ ◄──────────────────────► │   MCP Server    │
│  (Spring AI)    │     /mcp 端点             │  (Spring AI)    │
│                 │                           │                 │
│  ChatClient     │   tools/list (发现工具)    │  @Tool 方法      │
│  + ToolCallback │   tools/call  (调用工具)   │  ToolCallback   │
└─────────────────┘                           └─────────────────┘
```

## 前置条件

1. JDK 17+
2. 一个运行中的 MCP Server（如本项目的 `cloud-ai-sample` 模块，端口 8888）
3. OpenAI 兼容的 API Key（如 DashScope）

## Step 1：添加 Maven 依赖

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>

    <!-- Spring AI OpenAI 模型（DashScope 兼容 OpenAI 协议） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>

    <!-- Spring AI MCP Client（核心依赖） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client</artifactId>
    </dependency>
</dependencies>
```

> `spring-ai-starter-mcp-client` 会自动配置 MCP Client，连接远程 Server 并注册发现的工具为 `ToolCallback`。

## Step 2：配置 application.yml

```yaml
server:
  port: 8890

spring:
  application:
    name: mcp-client-sample
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        model: qwen3.8-max
        temperature: 0.7
    mcp:
      client:
        name: my-mcp-client           # Client 名称
        type: sync                     # 同步模式（也支持 async）
        streamable-http:
          connections:
            my-server:                 # 连接名称（可配置多个）
              url: http://localhost:8888  # MCP Server 地址
              endpoint: /mcp           # MCP 端点路径
```

**配置说明：**

| 配置项 | 说明 |
|--------|------|
| `spring.ai.mcp.client.name` | MCP Client 名称，用于初始化握手 |
| `spring.ai.mcp.client.type` | `sync`（同步）或 `async`（异步） |
| `spring.ai.mcp.client.streamable-http.connections` | 远程 MCP Server 连接列表，可配置多个 |
| `connections.<name>.url` | MCP Server 的基础 URL |
| `connections.<name>.endpoint` | MCP 端点路径，默认 `/mcp` |

## Step 3：编写 Controller

MCP Client 的核心用法是通过 `ToolCallbackProvider` 将远程工具注入 `ChatClient`：

```java
@RestController
@RequestMapping("/mcp")
public class McpChatController {

    private final ChatClient chatClient;

    /**
     * @param chatClientBuilder ChatClient 构建器
     * @param mcpToolCallbacks  MCP Client 自动注册的工具回调（来自远程 MCP Server）
     */
    public McpChatController(ChatClient.Builder chatClientBuilder, 
                             ToolCallbackProvider mcpToolCallbacks) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个具备实时信息获取能力的智能助手。")
                .defaultTools(mcpToolCallbacks)  // 注入远程 MCP Server 的所有工具
                .build();
    }

    /** 同步聊天 — AI 自动选择并调用 MCP Server 上的工具 */
    @RequestMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /** 流式聊天（SSE） */
    @RequestMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
```

**关键点：**

- `ToolCallbackProvider mcpToolCallbacks` 由 Spring AI MCP Client 自动配置，无需手动创建
- `.defaultTools(mcpToolCallbacks)` 将远程工具注入 ChatClient，AI 会自动选择并调用
- 支持同步（`.call()`）和流式（`.stream()`）两种模式

## Step 4：启动应用

```java
@SpringBootApplication
public class McpClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }
}
```

启动前确保 MCP Server 已运行（如 `cloud-ai-sample`，端口 8888）。

## Step 5：验证

```shell
# 配置 API Key
export OPENAI_API_KEY=your-api-key-here

# 启动 MCP Client
./mvnw spring-boot:run

# 测试：AI 会自动调用 MCP Server 上的时间工具
curl --max-time 60 --get --data-urlencode "message=现在几点了" \
  "http://localhost:8890/mcp/chat"

# 测试：AI 会自动选择搜索工具
curl --max-time 60 --get --data-urlencode "message=最近有什么新上映的电影" \
  "http://localhost:8890/mcp/chat"

# 测试：流式响应
curl --max-time 60 --get --data-urlencode "message=距离2026年春节还有多少天" \
  "http://localhost:8890/mcp/chat/stream"
```

## 工作原理

1. **启动阶段**：MCP Client 自动连接配置的 MCP Server，通过 `tools/list` 发现所有可用工具
2. **工具注册**：发现的工具被转换为 Spring AI 的 `ToolCallback`，通过 `ToolCallbackProvider` Bean 暴露
3. **请求阶段**：用户发送消息 → ChatClient 将消息和工具列表发送给大模型 → 模型决定调用哪个工具 → MCP Client 通过 `tools/call` 远程调用 Server 上的工具 → 工具结果返回给模型 → 模型生成最终回答
4. **多 Server 支持**：`connections` 下可配置多个 Server，所有 Server 的工具会合并注入
