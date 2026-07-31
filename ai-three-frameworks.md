# 一个 Java 开发者的 AI 三框架实践：LangChain4j、Spring AI 与 AgentScope 的选型、对比与互联

> **whatsmars-ai**（LangChain4j）| **spacecloud**（Spring AI 2.0）| **babi 系列**（AgentScope / LangGraph）
>
> 不做框架评测，不写 Hello World。本文记录的是我在三个真实项目中使用三个 Java AI 框架的工程实践——为什么选、怎么用、差异在哪、以及如何通过 MCP 协议让它们跨框架互联。

---

## 为什么是三个框架？

2025 年，Java 生态的 AI 集成框架格局基本定格：LangChain4j 走声明式路线，Spring AI 走 Spring 生态融合路线，AgentScope 走 Agent-as-a-Service 路线。三者设计哲学不同，适用场景也不同。

我没有选一个"通吃"，而是按技术方向拆分了三个项目，各司其职：

| 项目 | 聚焦方向 | 核心框架 | 语言 | 端口 |
|------|---------|---------|------|------|
| **whatsmars-ai** | AI 框架集成 | LangChain4j | Java | 8887 |
| **spacecloud** | AI + 微服务 | Spring AI 2.0 | Java | 8888/8889 |
| **babi 系列** | AI Coding Agent | AgentScope / LangGraph | Java / Python | - |

这不是为了"都用一遍"而刻意拆分。三个项目的定位本身就不同：whatsmars-ai 探索 LangChain4j 的声明式 AI Service 能做到多优雅；spacecloud 验证 Spring AI 在微服务场景下的工程化能力；babi 系列则面向更上层的 Agent 架构，实践 ReAct 模式的 Coding Agent。

**选型的核心逻辑是：用框架擅长的方式做它擅长的事。**

---

## 三框架设计哲学：声明式、生态融合、Agent-as-a-Service

### LangChain4j：声明式 AI Service

LangChain4j 最核心的抽象是 `@AiService` 接口。你定义一个 Java 接口，加一个 `@SystemMessage`，框架自动生成代理实现：

```java
@AiService
public interface SimpleAssistant {
    @SystemMessage("你是一个专业的 Java 技术专家，回答要简洁、准确。")
    String chat(String userMessage);
}
```

不需要手动构建 ChatClient、不需要写 prompt 拼接逻辑、不需要管理请求对象。接口方法的参数就是用户输入，返回值就是 AI 回复。这种"接口即 AI"的设计让 AI 调用看起来像普通的 Java 方法调用。

当你需要更细粒度的控制时，可以切换到编程式 `AiServices.builder()`：

```java
return AiServices.builder(ToolCallingAssistant.class)
        .streamingChatModel(streamingChatModel)
        .tools(timeTool, httpRequestTool, webSearchTool, systemInfoTool)
        .hallucinatedToolNameStrategy(hallucinatedToolNameStrategy)
        .build();
```

LangChain4j 的设计理念是：**让 AI 能力成为 Java 类型系统的一部分**。接口签名即 AI 接口契约，编译器帮你检查参数类型，IDE 帮你自动补全。这对 Java 开发者来说是最自然的接入方式。

### Spring AI 2.0：生态融合

Spring AI 不造新概念，而是把 AI 能力融入已有的 Spring 编程模型。核心是 `ChatClient` 的 fluent API：

```java
chatClient.prompt()
    .system("你是一个智能助手...")
    .user(message)
    .tools(timeTool, httpRequestTool, webSearchTool)
    .call()
    .content();
```

看起来和 LangChain4j 差别不大？真正的差异在架构层面。Spring AI 2.0 把 Tool Calling 提升为 **Advisor 链**的一环——`ToolCallingAdvisor`（order +300）和 `MessageChatMemoryAdvisor` 一样，是 ChatClient 调用链上的一个拦截器：

```
ChatClient → [ToolCallingAdvisor (+300)] → [自定义 Advisor (+400)] → ChatModel
```

这意味着 Tool Calling 不再是黑盒，你可以插入自己的 Advisor 来观测、拦截、修改工具调用的全过程。在 spacecloud 中，我写了一个 `ToolCallObservationAdvisor` 来记录每轮 ReAct 循环的工具调用请求、响应和耗时：

```java
@Override
public AdvisedResponse adviseRequest(AdvisedRequest request, AdvisorChain chain) {
    // 统计当前是第几轮工具调用
    int iteration = countToolResponseMessages(request.messages());
    log.info("ReAct 第 {} 轮: {}", iteration, request.toolContext());
    return chain.next(request);
}
```

Spring AI 的设计理念是：**AI 是 Spring 生态的一等公民，用 Spring 的方式（Bean、Advisor、自动配置）来管理 AI 能力**。如果你已经在用 Spring Boot，Spring AI 几乎零学习成本。

### AgentScope：Agent-as-a-Service

AgentScope 走了一条完全不同的路。它不关注"如何调用大模型"，而是关注"如何构建一个能自主工作的 Agent"。在 babi 系列中，AgentScope Java 的 `HarnessAgent` 内部实现了完整的 ReAct 循环：

```java
HarnessAgent.builder()
    .name("BabiAgent")
    .model(DashScopeChatModel.builder().stream(true).build())
    .toolkit(toolkit)
    .workspace(workspacePath)
    .maxIters(20)
    .maxRetries(2)
    .fallbackModel("qwen-turbo")
    .enableTaskList()
    .enablePlanMode()
    .middleware(new ContextTruncateMiddleware(30))
    .build();
```

注意这里没有 `.tools()` 的手动注册——Read、Write、Edit、Grep、Glob、Bash 这六个文件系统工具是框架内置的，通过 `LocalFilesystemSpec` 自动提供。你只需要注册业务专属的 4 个自定义工具。

AgentScope 还提供了其他框架没有的 Agent 级能力：Plan Mode（先调查后执行）、Task List（任务拆解）、Model Fallback（主模型失败自动降级）、Middleware Pipeline（中间件链）。这些不是 API 层的封装，而是 Agent 层的基础设施。

AgentScope 的设计理念是：**Agent 是一个独立的运行实体，配置好就能自主工作，开发者不应该手写 ReAct 循环。**

---

## 同一能力，三种实现

理论的对比说完，来看实战。同一个 AI 能力在三个框架里怎么实现。

### Tool Calling：注解风格 vs Advisor 链 vs 框架内置

Tool Calling 是三个框架差异最明显的能力。

**LangChain4j** 用 `@Tool` + `@P` 注解，工具是普通 Java 类，通过 `AiServices.builder().tools()` 注册：

```java
@Component
public class TimeTool {
    @Tool(name = "get_current_date_time")
    public String getCurrentDateTime() { ... }

    @Tool(name = "days_until")
    public String daysUntil(@P("目标日期，格式为 yyyy-MM-dd") String targetDate) { ... }
}
```

一个亮点：LangChain4j 支持 `hallucinatedToolNameStrategy`——当 LLM 幻觉出一个不存在的工具名时，不抛异常，而是返回错误信息让 LLM 自我纠正：

```java
@Bean
public Function<ToolExecutionRequest, ToolExecutionResultMessage> hallucinatedToolNameStrategy() {
    return req -> ToolExecutionResultMessage.from(req,
        "错误：没有名为 " + req.name() + " 的工具，请检查可用工具列表");
}
```

**Spring AI** 也用 `@Tool` + `@ToolParam` 注解，但工具注册是通过 `.tools()` 方法挂到 ChatClient 上，背后由 `ToolCallingAdvisor` 驱动：

```java
@Component
public class TimeTool {
    @Tool(name = "get_current_date_time", description = "获取当前的日期和时间")
    public String getCurrentDateTime() { ... }

    @Tool(name = "days_until", description = "计算今天距离目标日期还有多少天")
    public String daysUntil(@ToolParam(description = "目标日期") String targetDate) { ... }
}

// 调用时注册
chatClient.prompt()
    .user(message)
    .tools(timeTool, httpRequestTool, webSearchTool)
    .call()
    .content();
```

注意一个细节：Spring AI 的 `@Tool` 注解有独立的 `description` 字段，而 LangChain4j 的 `@Tool` 用 `value` 描述。参数注解也不同：`@ToolParam` vs `@P`。

**AgentScope** 则完全不同——6 个文件系统工具（Read/Write/Edit/Grep/Glob/Bash）是框架内置的，通过 `LocalFilesystemSpec` 自动注入，零代码。你只写业务工具：

```java
Toolkit toolkit = Toolkit.builder()
    .with(FileSystemToolkitSpec.defaults())  // 内置 6 个文件系统工具
    .tool(new FetchUrlTool())
    .tool(new HttpRequestTool())
    .tool(new GitHubApiTool())
    .tool(new SkillTool())
    .build();
```

三种风格的本质差异：LangChain4j 是"接口+注解"的 Java 经典范式；Spring AI 是"Bean+Advisor"的 Spring 原生范式；AgentScope 是"配置即拥有"的框架内置范式。

### Chat Memory：从内存到持久化

**LangChain4j** 的 Chat Memory 通过 `@MemoryId` 注解实现会话隔离，`ChatMemoryStore` 接口实现持久化：

```java
@AiService
public interface ChatMemoryAssistant {
    @SystemMessage("你是一个友好的技术助手...")
    TokenStream chat(@MemoryId String sessionId, @UserMessage String message);
}
```

在 whatsmars-ai 中，我实现了 `JpaChatMemoryStore`——用 JPA + PostgreSQL 持久化对话历史，每条消息序列化为 JSON 存入 `chat_memory` 表，按 `messageIndex` 保序：

```java
@Bean
public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
    return memoryId -> MessageWindowChatMemory.builder()
            .id(memoryId)
            .maxMessages(20)
            .chatMemoryStore(chatMemoryStore)
            .build();
}
```

`langchain4j-spring-boot-starter` 检测到 `ChatMemoryProvider` Bean 后，自动注入到所有使用 `@MemoryId` 的 `@AiService` 接口中——零配置。

**Spring AI** 的 Chat Memory 通过 `MessageChatMemoryAdvisor` 实现，会话 ID 通过 Advisor 参数传递：

```java
this.chatMemory = MessageWindowChatMemory.builder()
        .chatMemoryRepository(new InMemoryChatMemoryRepository())
        .maxMessages(20)
        .build();

this.chatClient = chatClientBuilder
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();

// 每次请求指定会话 ID
chatClient.prompt()
        .user(userMessage)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
        .call()
        .content();
```

在 spacecloud 的 RAG 模块中，Chat Memory 切换为 JDBC 持久化——只需把 `InMemoryChatMemoryRepository` 换成自动配置的 `JdbcChatMemoryRepository`：

```java
public ChatMemoryService(ChatClient.Builder chatClientBuilder,
                         ChatMemoryRepository chatMemoryRepository) {  // JDBC 自动注入
    this.chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)  // PostgreSQL
            .maxMessages(20)
            .build();
}
```

同一个接口 `ChatMemoryRepository`，内存版和 JDBC 版一键切换，这就是 Spring AI 自动配置的优势。

**AgentScope** 的会话持久化是 Agent 级别的，不是对话级别的。babi-agent 用 `JsonFileAgentStateStore` 把整个 Agent 状态（包括对话历史）序列化为 JSON 文件，重启后恢复：

```java
.stateStore(new JsonFileAgentStateStore(
        Paths.get(System.getProperty("user.home"), ".babi", "sessions")))
```

Python 版的 babi-langgraph 用 PostgreSQL Checkpointer，babi-agentscope 用 Redis Storage——不同框架有不同的持久化后端，但都是 Agent 状态级别的，比单纯的 Chat Memory 粒度更大。

### RAG：自动注入 vs 手动编排

**LangChain4j** 的 RAG 是声明式的——`@AiService` 接口配合 `ContentRetriever` Bean，框架自动完成"检索→注入上下文→生成回答"的全流程：

```java
@AiService
public interface RagAssistant {
    @SystemMessage("请严格基于检索到的知识库内容回答问题...")
    TokenStream chat(String userMessage);
}

// 配置 ContentRetriever
@Bean
public ContentRetriever contentRetriever(EmbeddingModel model, EmbeddingStore<TextSegment> store) {
    return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(model)
            .maxResults(5)
            .minScore(0.5)
            .build();
}
```

`RagAssistant.chat()` 被调用时，LangChain4j 自动 embed 用户查询、搜索向量库、将 top-5 结果注入 prompt——开发者完全不用管 RAG 的流程编排。

**Spring AI** 的 RAG 则是手动编排的——开发者显式地检索、拼接、生成：

```java
public String query(String question, int topK) {
    List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder().query(question).topK(topK).build());
    String context = docs.stream().map(Document::getText).collect(joining("\n\n---\n\n"));
    String prompt = "基于以下参考资料回答：\n" + context + "\n\n问题：" + question;
    return chatClient.prompt().user(prompt).call().content();
}
```

哪种更好？LangChain4j 的自动注入更简洁，适合标准 RAG 场景；Spring AI 的手动编排更灵活，适合需要对检索结果做二次处理（如重排序、过滤、混合检索）的场景。

### 流式输出：TokenStream vs Flux vs EventStream

三个框架都支持流式输出，但 API 风格迥异。

**LangChain4j** 返回 `TokenStream` 对象，通过回调链消费：

```java
TokenStream stream = assistant.chat(message);
stream.onPartialResponse(token -> emitter.send(token))
      .onCompleteResponse(response -> emitter.complete())
      .onError(emitter::completeWithError)
      .start();
```

**Spring AI** 返回 Reactor `Flux<String>`，直接对接 WebFlux/SSE：

```java
return chatClient.prompt()
        .user(message)
        .stream()
        .content();  // Flux<String>
```

**AgentScope** 返回事件流，通过 `streamEvents()` 迭代：

```java
agent.streamEvents(userMessage)
    .filter(e -> e.type() == EventType.TEXT_BLOCK_DELTA)
    .map(e -> e.delta().text())
    .subscribe(emitter::send);
```

Spring AI 的 `Flux<String>` 最简洁，直接返回给 Controller 就是 SSE 接口；LangChain4j 的回调链需要手动桥接到 SSE；AgentScope 的事件流最灵活，可以同时处理文本 token 和工具调用事件。

---

## MCP：让 LangChain4j 和 Spring AI 跨框架互联

前面讲的都是在各自框架内部的能力实现。但三个框架之间不是孤岛——MCP 协议让它们可以跨框架互联。

在我的项目矩阵中，有一条 MCP 通信链：

```
whatsmars-ai (LangChain4j MCP Client, 端口 8887)
    ↓ Streamable HTTP
whatsmars-mcp (Spring AI MCP Server, 端口 8886)
    暴露工具: get_weather, show_location, plan_route, search_place
```

**MCP Server 端**（Spring AI）的配置极其简洁——`MethodToolCallbackProvider` 把 `@Tool` 方法自动注册为 MCP 工具：

```java
@Configuration
public class McpServerConfig {
    @Bean
    public ToolCallbackProvider mcpToolProvider(WeatherTool weatherTool, MapTool mapTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherTool, mapTool)
                .build();
    }
}
```

`application.yml` 指定 Streamable HTTP 协议：

```yaml
spring:
  ai:
    mcp:
      server:
        name: whatsmars-mcp
        version: 1.0.0
        type: SYNC
        protocol: streamable
```

**MCP Client 端**（LangChain4j）通过 `StreamableHttpMcpTransport` 连接 Server，再用 `McpToolProvider` 把远程工具适配为 LangChain4j 的工具：

```java
@Bean(destroyMethod = "close")
public McpClient mcpClient() {
    McpTransport transport = StreamableHttpMcpTransport.builder()
            .url("http://localhost:8886/mcp")
            .logRequests(true)
            .logResponses(true)
            .build();
    return new DefaultMcpClient.Builder()
            .key("whatsmars-mcp")
            .transport(transport)
            .build();
}

@Bean
public McpToolProvider mcpToolProvider(McpClient mcpClient) {
    return McpToolProvider.builder().mcpClients(mcpClient).build();
}

@Bean
public McpAssistant mcpAssistant(StreamingChatModel model, McpToolProvider provider) {
    return AiServices.builder(McpAssistant.class)
            .streamingChatModel(model)
            .toolProvider(provider)  // MCP 远程工具作为 LangChain4j 工具
            .build();
}
```

从 McpAssistant 的视角看，它不知道这些工具是本地的 Java 方法还是远程的 MCP 服务——LangChain4j 的 `ToolProvider` 接口抹平了这个差异。用户问"北京今天天气怎么样"，LangChain4j 的 LLM 决定调用 `get_weather` 工具，`McpToolProvider` 通过 HTTP 请求 whatsmars-mcp Server，Spring AI 执行 `WeatherTool.getWeather()` 方法，结果原路返回。

**一份 `@Tool` 代码，两个框架共享。** 这就是 MCP 的价值——不是替代 Tool Calling，而是让 Tool Calling 跨越框架边界。

在 spacecloud 的 cloud-ai-sample 模块中，同样配置了一个 MCP Server（暴露 TimeTool、HttpRequestTool、WebSearchTool），这意味着任何 MCP Client（不管是 LangChain4j、Claude、还是其他 Agent）都可以调用 spacecloud 的工具能力。

---

## 选型建议：不要用框架不擅长的方式

经过三个项目的实践，我的选型建议很明确：

**选 LangChain4j，如果你追求声明式的优雅。** `@AiService` 接口让 AI 调用像写 Java 方法一样自然，`@MemoryId` 和 `ContentRetriever` 的自动注入让多轮对话和 RAG 几乎零配置。它最适合"AI 能力集成"场景——在你的应用里加一个 AI 对话、RAG 问答、工具调用的模块。whatsmars-ai 就是这种定位。

**选 Spring AI，如果你的项目已经在 Spring 生态里。** ChatClient 的 fluent API、Advisor 链的拦截能力、Spring Boot 的自动配置——如果你已经在用 Spring Boot 微服务，Spring AI 是最自然的选择。它的优势不在 API 简洁度（这方面 LangChain4j 更胜一筹），而在和 Spring Cloud、Spring Data、Spring Security 等生态组件的融合。spacecloud 把 AI 能力嵌入 16 个微服务模块中，Spring AI 的生态融合优势在这里体现得淋漓尽致。

**选 AgentScope，如果你要构建自主 Agent。** HarnessAgent 内置的 ReAct 循环、Plan Mode、Task List、Model Fallback、Middleware Pipeline——这些是其他两个框架不提供的 Agent 级基础设施。如果你要做的不是"在应用里加 AI 能力"，而是"构建一个 AI Agent"，AgentScope 让你不用手写 Agent 的底层循环。babi 系列就是这种定位。

**不要用框架不擅长的方式。** 在 babi-spring（Spring AI 版 Coding Agent）中，我试图用 `ChatClient` + `ToolCallingAdvisor` 实现 Coding Agent，但遇到了一个硬伤：Spring AI 在工具调用期间会阻塞流式输出——Agent 在执行工具时，用户看不到任何中间反馈。而 AgentScope 的 `streamEvents()` 可以同时推送文本 token 和工具调用事件，用户体验完全不同。

这不是 Spring AI 的"bug"，而是设计取向的差异：Spring AI 优化的是"在 Spring 应用里调用 AI"的体验，不是"构建自主 Agent"的体验。用 Spring AI 写 Agent，就像用 Spring MVC 写实时游戏服务器——能跑，但不是它擅长的。

---

## 技术栈全景

三个项目共享相同的大模型和基础设施：

| 维度 | 选型 | 说明 |
|------|------|------|
| 大模型 | 通义千问 qwen-plus | 通过 DashScope OpenAI 兼容 API 接入 |
| 视觉模型 | qwen3.7-plus | 多模态图片理解 |
| Embedding | text-embedding-v3 | 1024 维，中文语义理解优秀 |
| 向量数据库 | PostgreSQL + pgvector | 关系数据和向量存储共库 |
| MCP 协议 | Streamable HTTP | 替代早期 SSE，支持有状态会话 |

不同框架的依赖版本：

| 框架 | 版本 | 所属项目 |
|------|------|---------|
| LangChain4j | 1.18.0 | whatsmars-ai |
| Spring AI | 2.0.0 | spacecloud |
| AgentScope Java | 2.0 | babi-agent |
| LangGraph4J | 1.8.20 | babi-langgraph4j |
| Spring Boot | 3.5.14 / 4.1.0 | whatsmars-ai / spacecloud |

---

## 项目地址

- **whatsmars**: [https://github.com/javahongxi/whatsmars](https://github.com/javahongxi/whatsmars) — whatsmars-ai 模块（LangChain4j）
- **spacecloud**: [https://github.com/javahongxi/spacecloud](https://github.com/javahongxi/spacecloud) — cloud-ai-sample / cloud-ai-rag-sample 模块（Spring AI 2.0）
- **babi**: [https://github.com/javahongxi/babi](https://github.com/javahongxi/babi) — AgentScope Java / LangGraph4J / Spring AI 三框架对比

---

## 写在最后

三个框架，三个项目，不是在做"框架评测"，而是在用不同的工具解决不同的问题。

LangChain4j 让 AI 调用像 Java 接口一样优雅，Spring AI 让 AI 能力像 Spring Bean 一样融入应用，AgentScope 让 Agent 构建像搭积木一样简单。它们不是竞争关系，而是分工关系。

而 MCP 协议的存在，让这种分工不再是技术债——LangChain4j 的 Client 可以调用 Spring AI 的 Server 暴露的工具，不同框架的 Agent 可以共享工具能力。**框架选型不应该是一个非此即彼的决定，而是一个组合拳的设计。**

如果你也在 Java 生态做 AI 集成，希望这篇实践记录能给你一些参考。不是告诉你"该选哪个"，而是告诉你"每个框架擅长什么"——然后你根据自己的场景做判断。

```bash
git clone https://github.com/javahongxi/whatsmars.git    # LangChain4j
git clone https://github.com/javahongxi/spacecloud.git   # Spring AI 2.0
git clone https://github.com/javahongxi/babi.git         # AgentScope / LangGraph
```

Star ⭐ 你感兴趣的项目，后续会持续更新三个框架的实践。

---

**© [hongxi.org](http://hongxi.org)** | 用三个框架，做三件不同的事
