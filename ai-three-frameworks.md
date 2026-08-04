# babi 工程化实践全链路：AgentScope vs LangGraph4j vs Spring AI 2.0 三大框架深度对比

> 本文基于 [javahongxi/babi](https://github.com/javahongxi/babi) 项目，从分层架构视角自底向上对比 AgentScope、LangGraph4j、Spring AI 2.0 三大框架在 AI Agent 工程化落地中的设计差异。babi 是面向开发者的 AI Coding Agent，三个模块共享同一套工具和系统提示，但分别用三种框架实现，是绝佳的横向对比样本。

## 架构全景

babi 的三个模块自底向上可以分为以下层级：

```
HTTP Client 层 → 流式传输层 → ChatModel 层 → Agent 层
  → 工具定义与调用层 → 事件拦截层 → ReAct 循环层
  → Chat Memory 层 → 可观测层 → Skill 加载层
```

每个层级三个框架各有一套实现，选型哲学截然不同。下面逐层剖析。

## 一、ChatModel HTTP Client 选型：谁在替你访问 LLM

三个模块的 ChatModel 底层各用不同的 HTTP Client 访问 LLM API，这是工程化中容易被忽视的一层——开发者通常只关注 ChatModel 接口，不太关心底层 HTTP 栈，但 HTTP Client 的选型直接影响连接管理、流式支持、超时控制和依赖体积。

先看 Spring AI 的两个 starter。`spring-ai-starter-model-deepseek` 使用 Spring 自带的 `RestClient`/`WebClient`——前者用于同步调用，后者用于流式调用。这套 HTTP 栈与 Spring 生态深度集成，连接池由 Spring 管理，不需要额外引入第三方 HTTP 库。`spring-ai-starter-model-openai` 则不同，它依赖 `openai-java-core`，底层用 OkHttp——一个功能丰富但体积较大的第三方 HTTP 库，会传递引入 OkHttp 及其依赖。

再看 babi 实际使用的两个 SDK。`dashscope-sdk-java` 默认使用 JDK 内置的 `java.net.http.HttpClient`，但它抽象了 `HttpTransport` 接口，允许注入自定义实现。切换逻辑是硬编码的——如果显式设置了 custom transport，就用 custom 的；否则用默认的 JDK HttpClient。这种设计简洁但不灵活，不支持多实现共存。

LangChain4j 的 `langchain4j-open-ai` 采用了类似但更灵活的方案——通过 SPI（Service Provider Interface）机制抽象 HTTP 传输层。SPI 的优势是按需加载：classpath 上有哪个实现的 META-INF/services 文件，就自动用哪个实现，不需要硬编码判断。默认实现取决于 classpath 上的依赖，通常引入了 OkHttp 就用 OkHttp，没有则 fallback 到 JDK HttpClient。

回到 babi 的三个模块。babi-agent 用 AgentScope 的 `DashScopeChatModel`，底层是 dashscope-sdk-java，走 JDK HttpClient。babi-graph 用 LangChain4j 的 `OpenAiStreamingChatModel`，底层走 SPI 机制——因为 pom 中引入了 `langchain4j-open-ai`，SPI 默认找到 OkHttp 实现。babi-spring 此前用 `spring-ai-starter-model-deepseek`，走 Spring RestClient/WebClient；这次更新切换到自定义 `DashScopeChatModel` 直接使用 dashscope-sdk-java，回到了 JDK HttpClient。

这三个选型的对比可以用一个维度概括：**HTTP 栈的控制力与框架耦合度成正比**。Spring 的 RestClient/WebClient 控制力最强（能复用 Spring 的连接池配置、超时拦截器、日志机制），但与 Spring 生态深度绑定。OkHttp 功能最丰富（支持拦截器、GZIP、HTTP/2、WebSocket），但引入额外依赖。JDK HttpClient 最轻量（零依赖、支持 HTTP/2），但功能受限——没有拦截器机制，调试不如 OkHttp 方便。dashscope-sdk-java 的 `HttpTransport` 抽象和 LangChain4j 的 SPI 机制都提供了可插拔能力，但前者的硬编码判断逻辑不如后者的 SPI 自动发现灵活。

babi-spring 从 RestClient/WebClient 切换到 JDK HttpClient 的深层原因是——自定义 `DashScopeChatModel` 需要完全控制流式 tool call 的处理逻辑，Spring RestClient/WebClient 的响应处理管道与 Spring AI 框架耦合太深，难以在中间插入 chunk 累积逻辑。而 dashscope-sdk-java 的 `Generation.streamCall()` 返回原始的 `Flowable<GenerationResult>`，babi 可以在 `convertToFlux` 中自由处理每个 chunk——累积 tool call、立即转发文本 delta——完全不受框架 HTTP 管道的约束。

## 二、流式传输层：Reactor 全链路 vs RxJava→Reactor 桥接 vs Java 原生回调

这一层关注的是：HTTP Client 收到 LLM 的流式响应（SSE 或 chunked）后，怎么把一块块的数据送到上层框架。三个框架在这层的选型截然不同。

**babi-spring（Spring AI 2.0）** 全程走 Reactor 生态，从 HTTP 到框架 API 无桥接。Spring AI 的 ChatModel 接口定义 `stream()` 返回 `Flux<ChatResponse>`，babi-spring 的自定义 `DashScopeChatModel` 实现这个接口。但 DashScope SDK 的 `generation.streamCall()` 返回的是 RxJava 的 `Flowable<GenerationResult>`——所以 babi-spring 在 ChatModel 层内部做了一次 RxJava → Reactor 桥接：

```java
public Flux<ChatResponse> stream(Prompt prompt) {
    // ...
    Flowable<GenerationResult> flowable = generation.streamCall(builder.build());
    return convertToFlux(flowable);
}

private Flux<ChatResponse> convertToFlux(Flowable<GenerationResult> flowable) {
    return Flux.<ChatResponse>create(sink -> {
        for (GenerationResult result : flowable.blockingIterable()) {
            // 处理 chunk，emit 到 Reactor sink
        }
        sink.complete();
    }).subscribeOn(Schedulers.boundedElastic());
}
```

桥接方式是 `flowable.blockingIterable()` 在 boundedElastic 线程上阻塞消费 RxJava Flowable，通过 `Flux.create()` 的 sink 逐个 emit 到 Reactor Flux。从这一层往上，`BabiService.streamChat()` 返回 `Flux<String>`，Controller 用 `Flux.merge()` 合并工具事件流和文本流，Spring AI 的 `ToolCallingAdvisor` 也在 Reactor 管道内完成 ReAct 循环。整个链路从 ChatModel 到 Controller 全是 Reactor，只有 DashScope SDK 内部用了 RxJava。

**babi-agent（AgentScope）** 同样使用 Reactor 库。AgentScope 的中间件 API 直接使用 `reactor.core.publisher.Flux`——`onActing` 和 `onReasoning` 方法都返回 `Flux<AgentEvent>`：

```java
@Override
public Flux<AgentEvent> onActing(Agent agent, RuntimeContext ctx,
        ActingInput input, Function<ActingInput, Flux<AgentEvent>> next) {
    // 发布工具事件
    return next.apply(input);  // Reactor Flux 链
}
```

`agent.streamEvents()` 也返回 Reactor 类型，Controller 通过 `.toIterable()` 阻塞遍历后桥接到 SSE。AgentScope 底层同样依赖 DashScope SDK 的 `Flowable`，但 AgentScope 框架内部已经将 Flowable 转成了 Reactor Flux——中间件开发者看到的 API 全是 Reactor。所以 babi-agent 的流式链路是：DashScope SDK（RxJava Flowable）→ AgentScope 内部转换（→ Reactor Flux）→ 中间件链（Reactor Flux）→ Controller（Reactor SSE）。

从 Reactor 使用深度看，babi-spring 和 babi-agent 都用 Reactor，但 AgentScope 框架内部已经抹平了 RxJava 和 Reactor 的差异，开发者只需面对 Reactor 一套 API。babi-spring 则需要开发者在 `DashScopeChatModel` 中自己写桥接逻辑。

**babi-graph（LangChain4j + LangGraph4j）** 走了一条完全不同的路线——不依赖任何响应式库，纯 Java 原生机制。LangChain4j 的设计哲学是尽量减少第三方依赖，流式传输用回调 + Java 队列实现。

LangChain4j 的 `OpenAiStreamingChatModel` 实现 `StreamingChatModel` 接口，核心方法签名是 `void stream(ChatRequest, StreamingResponseHandler handler)`——注意返回值是 void，流式数据通过回调接口推送：

```java
public interface StreamingResponseHandler<T> {
    void onPartialResponse(String partial);    // 每个文本 chunk
    void onCompleteResponse(T complete);        // 流结束
    void onError(Throwable error);              // 异常
}
```

底层 HTTP Client 收到 LLM 的 SSE 响应后，解析方式取决于 SPI 加载的是哪个实现。如果 classpath 上有 OkHttp（如 babi-graph 通过 `langchain4j-open-ai` 传递引入），OkHttp 的 `EventSource` + `EventSourceListener` 自动处理 SSE 协议——解析 `data:` 前缀、事件分帧、`\n\n` 边界识别，通过 `onEvent()` 回调推送解析后的事件。如果 SPI fallback 到 JDK HttpClient，则没有现成的 EventSource——LangChain4j 的 JDK HTTP client 模块需要自己实现 SSE 解析，通过 `BodySubscribers.ofLines()` 拿到 `Stream<String>` 逐行读取，手动拼接多行 data、识别事件边界。但无论用哪个 HTTP Client，解析后的数据都通过 `StreamingResponseHandler` 回调推送到上层——`onPartialResponse()` 推送文本 chunk，`onCompleteResponse()` 通知流结束，`onError()` 报告异常。整个过程在 HTTP Client 的回调线程上执行，没有 Reactor Flux 也没有 RxJava Flowable——就是最朴素的观察者模式，HTTP Client 的选择只影响最底层的 SSE 字节解析，上层的回调机制不变。

LangGraph4j 在此之上构建图流式输出。`CompiledGraph.stream()` 返回 `AsyncGenerator`，可以被消费为 `java.util.stream.Stream`。`AsyncGenerator` 内部通过阻塞队列将回调线程上的数据传递到消费线程——生产者（HTTP Client 回调线程）push 数据到队列，消费者（图遍历线程）从队列 poll 数据：

```java
var result = graph.stream(input, config);
result.stream()
        .filter(output -> output instanceof StreamingOutput<?>)
        .map(output -> (StreamingOutput<?>) output)
        .forEach(so -> {
            String chunk = so.chunk();
            // chunk 来自 AsyncGenerator 的阻塞队列
        });
```

babi-graph 的 `BabiService` 需要把这个 Java Stream 桥接到 Spring WebFlux 的 Reactor Flux，方式是创建 `Sinks.Many` + 独立线程：

```java
Sinks.Many<Map<String, Object>> sink = Sinks.many().unicast().onBackpressureBuffer();

new Thread(() -> {
    try {
        var result = graph.stream(input, config);
        result.stream()
                .filter(output -> output instanceof StreamingOutput<?>)
                .map(output -> (StreamingOutput<?>) output)
                .forEach(so -> {
                    String chunk = so.chunk();
                    if (chunk != null && !chunk.isEmpty()) {
                        sink.tryEmitNext(Map.of("type", "token", "data", chunk));
                    }
                });
        sink.tryEmitComplete();
    } catch (Exception e) {
        sink.tryEmitNext(Map.of("type", "error", "data", e.getMessage()));
        sink.tryEmitComplete();
    }
}, "babi-agent-" + sessionId).start();

return sink.asFlux();
```

这是三个模块中桥接最重的——需要独立线程消费 Java Stream，通过 Sinks 推送到 Reactor Flux。ThreadLocal 在这里能工作，因为工具执行和图遍历在同一线程。

三种方案的对比总结：Spring AI 和 AgentScope 都采用 Reactor 库作为流式基础设施，但 AgentScope 在框架层已经封装了 DashScope SDK 的 RxJava Flowable，开发者只需面对 Reactor；babi-spring 需要开发者在 ChatModel 层手动桥接。LangChain4j 刻意避开响应式库，用 OkHttp 回调 + Java 阻塞队列实现流式传输，这种设计的优势是依赖轻量、调试直观（回调栈可读），劣势是需要额外桥接才能对接 Spring WebFlux 的 Reactor SSE，且独立线程的创建开销在高并发下不可忽视。

## 三、ChatModel 层：原生 SDK vs OpenAI 兼容 vs 框架内置

**babi-spring** 自定义 `DashScopeChatModel` 实现 Spring AI 的 `ChatModel` 接口，直接使用 `dashscope-sdk-java` 原生 API。核心挑战是流式 tool call chunk 的累积合并——DashScope 流式返回时将一个 tool call 拆成多个 chunk，需要先在 `AccumulatedToolCall` 中累积 id/name/arguments，等流结束后一次性发出完整的工具调用响应。这个实现此前已详细解析，此处不再展开。

**babi-graph** 用 LangChain4j 的 `OpenAiStreamingChatModel`，走 OpenAI 兼容协议。DashScope 特有参数（如 `enable_search`）通过 `OpenAiChatRequestParameters.customParameters` 传入，这是一个兼容层的 workaround：

```java
Map<String, Object> mergedCustomParams = chatModelProperties.customParameters();
if (Boolean.TRUE.equals(chatModelProperties.enableSearch())) {
    mergedCustomParams = new HashMap<>(mergedCustomParams);
    mergedCustomParams.put("enable_search", true);
}

return OpenAiStreamingChatModel.builder()
        .defaultRequestParameters(OpenAiChatRequestParameters.builder()
                .customParameters(mergedCustomParams)
                .build())
        .build();
```

**babi-agent** 用 AgentScope 的 `DashScopeChatModel.builder()`，这是框架原生支持，直接配置 `enableSearch` 即可，最简洁：

```java
.model(DashScopeChatModel.builder()
        .apiKey(properties.apiKey())
        .modelName(properties.chat().model())
        .stream(true)
        .enableSearch(properties.chat().enableSearch())
        .build())
```

三个模块的对比结论是：框架原生支持程度 AgentScope > Spring AI（自定义）> LangChain4j（兼容层）。但 Spring AI 的自定义实现控制力最强——流式 tool call 的处理完全自主，不受框架对 chunk 格式的假设约束。babi-spring 的 `DashScopeChatModel` 虽然实现量大（约 300 行），但获得了对流式 tool call 的完整控制——可以自定义 chunk 累积策略、消息转换逻辑、工具定义映射方式。如果未来需要支持 DashScope 的新特性（如多模态、推理链），只需在 `DashScopeChatModel` 中增加映射逻辑，不受 Spring AI 框架的更新节奏约束。

babi-graph 的 OpenAI 兼容方式有一个隐性风险——LangChain4j 的 `OpenAiStreamingChatModel` 对 chunk 格式有特定假设，如果 DashScope 的 OpenAI 兼容端点在某些边界情况下与 OpenAI 格式不完全一致，可能触发与 babi-spring 此前遇到的相同问题（function 字段为 null）。babi-graph 目前能工作，部分原因是 LangChain4j 的工具调用处理比 Spring AI 更宽容，但这不意味着根本性的兼容问题已解决。这也解释了为什么 babi-spring 选择自定义 ChatModel 而非继续用兼容层。

## 四、Agent 层：HarnessAgent vs CompiledGraph vs ChatClient

三个框架对"Agent"的定义截然不同。

**babi-agent 的 HarnessAgent** 是最"重量级"的。它内置了文件系统访问（`LocalFilesystemSpec`）、会话状态持久化（`AgentStateStore`）、Plan Mode、任务列表（TaskList）、内存管理（Memory Hooks）、上下文压缩（Compaction）等开箱即用的能力。配置时通过 builder 链式调用开启或关闭各种内置功能：

```java
return HarnessAgent.builder()
        .name(AgentUtils.AGENT_NAME)
        .sysPrompt(sysPrompt)
        .model(...)
        .fallbackModel(properties.chat().fallbackModel())
        .toolkit(toolkit)
        .workspace(workspacePath)
        .filesystem(new LocalFilesystemSpec()
                .project(workspacePath)
                .mode(LocalFsMode.UNRESTRICTED))
        .stateStore(stateStore)
        .maxIters(50)
        .maxRetries(2)
        .enableTaskList()
        .enablePlanMode()
        .allowShellInPlanMode()
        .disableDynamicSkills()
        .disableMemoryTools()
        .disableMemoryHooks()
        .disableCompaction()
        .disableToolResultEviction()
        .enableAgentTracingLog(false)
        .middleware(new ContextTruncateMiddleware(30))
        .middleware(new ToolNotificationMiddleware(toolEventBus))
        .build();
```

这个"全家桶"式设计的好处是开箱即用，代价是灵活性受限——不能像 Spring AI 那样自由组装 Advisor 链。但中间件机制（Middleware）提供了扩展点，可以插入 `ContextTruncateMiddleware`（上下文截断）和 `ToolNotificationMiddleware`（工具事件通知）。值得注意的是 babi 显式关闭了多个内置功能（`disableMemoryHooks`、`disableCompaction`、`disableToolResultEviction`），说明框架的"全家桶"能力并非全部需要——babi 用自己的 `ContextTruncateMiddleware` 替代框架的 Compaction，用自己的 `SkillTool` 替代框架的 DynamicSkills。这种"框架打底 + 自定义替换"的策略在大型项目中很常见。

**babi-graph 的 CompiledGraph** 是图编排范式。先构建 AgentExecutor 图（含 agent 节点和 action 节点），编译后得到不可变的 `CompiledGraph`，通过 `MemorySaver` 做检查点持久化：

```java
var graph = AgentExecutor.builder()
        .chatModel(streamingChatModel)
        .systemMessage(SystemMessage.from(sysPrompt))
        .toolsFromObject(tools.toArray())
        .build();

graph.addWrapCallEdgeHook(Agent.ACTION_LABEL, new ToolNotificationEdgeHook(toolEventBus));

return graph.compile(CompileConfig.builder()
        .checkpointSaver(memorySaver)
        .recursionLimit(50)
        .build());
```

图编排的优势是可视化清晰、可插入 edge hook、支持检查点恢复。劣势是 ReAct 循环由图结构固定，不如 Spring AI 的 Advisor 链灵活。`recursionLimit(50)` 限制了图的最大遍历深度，防止无限循环。

**babi-spring 的 ChatClient** 是最"轻量"的。Spring AI 2.0 的 ChatClient 本身不是 Agent，只是一个聊天客户端，ReAct 循环由 `ToolCallingAdvisor` 在 Advisor 链中完成。babi-spring 通过组装三个 Advisor 实现完整的 Agent 能力：

```java
return ChatClient.builder(chatModel)
        .defaultSystem(sysPrompt)
        .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                ToolCallingAdvisor.builder()
                        .toolCallingManager(toolCallingManager)
                        .build(),
                new ToolCallObservationAdvisor()
        )
        .defaultTools(tools.toArray())
        .build();
```

`MessageChatMemoryAdvisor` 管理会话记忆，`ToolCallingAdvisor` 驱动 ReAct 循环，`ToolCallObservationAdvisor` 做可观测。这种"组合优于继承"的设计让每个关注点独立，可以自由替换。

## 五、工具定义与调用：三种 @Tool 注解

三个框架都用 `@Tool` 注解定义工具，但注解来源和参数注入方式不同。

**babi-agent（AgentScope）** 用 `io.agentscope.core.tool.Tool` 和 `@ToolParam`，方法签名可以接收 `RuntimeContext` 实现上下文注入：

```java
@Tool(name = "github_api_request", description = "Call the GitHub REST API...")
public String githubApiRequest(
        RuntimeContext runtimeContext,
        @ToolParam(name = "method", description = "HTTP method") String method,
        @ToolParam(name = "path", description = "GitHub API path") String path,
        @ToolParam(name = "body", required = false) String body,
        @ToolParam(name = "query_params", required = false) Map<String, String> queryParams) {
    String token = resolveToken(runtimeContext);
    return GitHubApiLogic.githubApiRequest(client, token, method, path, body, queryParams);
}
```

**babi-graph（LangChain4j）** 用 `dev.langchain4j.agent.tool.Tool` 和 `@P`，注解最简洁：

```java
@Tool(name = "read_file", value = "Read the contents of a file...")
public String readFile(@P("Absolute or relative path") String filePath) {
    return FileReadLogic.readFile(filePath);
}
```

**babi-spring（Spring AI 2.0）** 用 `org.springframework.ai.tool.annotation.Tool` 和 `@ToolParam`：

```java
@Tool(name = "read_file", description = "Read the contents of a file...")
public String readFile(
        @ToolParam(description = "Absolute or relative path") String filePath) {
    return FileReadLogic.readFile(filePath);
}
```

尽管注解不同，三个模块的工具类都委托给 `babi-common` 中的 Logic 类执行实际逻辑。这种"薄工具层 + 共享逻辑层"的设计确保了行为一致性——无论用哪个框架，`read_file` 工具的行为完全相同。差异只在参数注入——AgentScope 能注入 `RuntimeContext`，另外两个不能。

注解只是工具定义，调用实现才是关键。三个框架从"模型返回 tool calls JSON"到"执行 Java 方法拿到返回值"的链路各不相同。

**babi-agent（AgentScope）** 通过 `Toolkit` 注册工具对象，框架用反射扫描 `@Tool` 注解方法生成工具规格（含 name、description、parameters JSON Schema），随 API 请求传给模型。模型返回 tool calls 后，AgentScope 在 `onActing` 中间件阶段执行——框架根据 tool name 找到对应的 Java 方法，通过反射将 JSON arguments 反序列化为方法参数，调用方法获取返回值。`RuntimeContext` 作为特殊参数被框架自动注入，不来自 JSON arguments。执行结果通过 `ToolResultEndEvent` 事件推送到下游。AgentScope 还内置了 `maxRetries(2)` 的工具重试机制——工具方法抛异常时自动重试，超出次数才返回错误给模型。

**babi-graph（LangChain4j）** 通过 `toolsFromObject(tools.toArray())` 注册工具对象，LangChain4j 用反射扫描 `@Tool` 注解生成 `ToolSpecification`。模型返回 tool execution requests 后，LangGraph4j 的 `AgentExecutor` 图进入 action 节点——`ToolExecutor` 根据 tool name 找到方法，用 Jackson 将 JSON arguments 反序列化为参数，反射调用方法。返回值序列化为 JSON 后写入图的 state，图再回到 agent 节点继续下一轮。整个调用链是图遍历驱动的，工具执行在图遍历线程上同步完成。

**babi-spring（Spring AI 2.0）** 通过 `defaultTools(tools.toArray())` 注册工具对象，Spring AI 用反射扫描 `@Tool` 注解生成 `ToolDefinition`（含 name、description、inputSchema JSON Schema）。模型返回 tool calls 后，`ToolCallingAdvisor` 调用 `ToolCallingManager.executeToolCalls()`——这是工具执行的核心入口。babi-spring 用自定义的 `NotifyingToolCallingManager` 装饰默认实现，在执行前后发布事件。默认的 `ToolCallingManager` 通过 `ToolCallbackResolver` 根据 tool name 解析到 `ToolCallback`，再由 `MethodToolCallback` 用反射调用 Java 方法。工具返回值包装为 `ToolResponseMessage.ToolResponse`，加入消息历史供下一轮模型调用。Spring AI 的工具执行没有内置重试机制——如果工具抛异常，异常直接传播到 `ToolCallingAdvisor`，需要开发者自己处理。

三种调用实现的核心差异在于工具执行与框架的耦合方式。AgentScope 的工具执行嵌入中间件链，可以被 `onActing` 拦截和修改；LangChain4j 的工具执行嵌入图遍历，可以被 `EdgeHook.WrapCall` 包裹；Spring AI 的工具执行通过 `ToolCallingManager` 装饰器解耦，是最灵活的——可以替换整个 `ToolCallingManager` 实现而不影响 Advisor 链。但三者的底层机制殊途同归——反射 + JSON 反序列化 + 方法调用，只是被框架包装在不同的抽象层中。

## 六、事件拦截机制：Middleware vs EdgeHook vs Advisor

三个框架拦截工具执行事件的机制完全不同，这是架构差异最直观的体现。

**babi-agent（AgentScope Middleware）** 在工具执行前拦截。`ToolNotificationMiddleware` 实现 `MiddlewareBase.onActing()`，在 Agent 调用工具前发布 `TOOL_CALL` 事件，然后调 `next.apply(input)` 继续。这是"前置拦截"模式，不阻塞执行，但不能在中间件内获取工具执行结果——结果事件需要从 `ToolResultEndEvent` 另外获取。

**babi-graph（LangGraph4j EdgeHook）** 拦截图的 action edge。`ToolNotificationEdgeHook` 实现 `EdgeHook.WrapCall`，通过 `whenComplete` 回调在工具执行完成后发布 `TOOL_RESULT` 事件。这是"包裹拦截"模式，前置发 TOOL_CALL，后置发 TOOL_RESULT，一次拦截覆盖全生命周期：

```java
public CompletableFuture<Command> applyWrap(String sourceId,
        AgentExecutor.State state, RunnableConfig config,
        AsyncCommandAction<AgentExecutor.State> action) {
    publishToolCallEvents(state);
    return action.apply(state, config)
            .whenComplete((command, ex) -> {
                publishToolResultEvents(state, resolveToolState(ex));
            });
}
```

**babi-spring（Spring AI Advisor + ToolCallingManager 装饰器）** 用两层拦截。`NotifyingToolCallingManager` 装饰 `ToolCallingManager`，在 `executeToolCalls` 前后分别发布事件，通过 try-catch 捕获异常判断 SUCCESS/ERROR/INTERRUPTED。这是"装饰器模式"，最灵活但代码量也最大。session 跨线程传播用双路策略——先 Reactor Context 后 ThreadLocal。

三种拦截机制的设计哲学不同。AgentScope 的 Middleware 是"管道-过滤器"模式，中间件串行执行，每个可以修改输入或阻断执行，适合做前置处理（如上下文截断、权限检查），但不适合做后置处理（如结果通知）——`onActing` 返回的 `Flux<AgentEvent>` 是工具执行的输出流，中间件无法在流结束后再插入逻辑。LangGraph4j 的 EdgeHook 是"包裹器"模式，`whenComplete` 回调天然覆盖前置和后置，一次拦截完成全生命周期通知，最简洁。Spring AI 的装饰器模式灵活性最高——可以修改工具调用的输入和输出，甚至完全替换工具执行逻辑——但复杂度也最高，需要理解 `ToolCallingManager` 的内部契约。

三个模块的工具状态判断逻辑也值得对比。babi-spring 的 `resolveToolState` 遍历异常 cause chain，匹配 `InterruptedException` 和 `CancellationException` 为 INTERRUPTED，其他为 ERROR。babi-graph 的 `resolveToolState` 更简洁——null 为 SUCCESS，cause chain 中有 `CancellationException` 为 INTERRUPTED，其他为 ERROR。babi-agent 不需要自己判断——AgentScope 框架的 `ToolResultEndEvent` 直接携带 `getState()`。这说明框架内置的能力能显著减少工程代码量。

## 七、ReAct 循环：内置 vs 图遍历 vs Advisor 递归

**babi-agent** 的 ReAct 循环由 AgentScope 框架内置。HarnessAgent 内部实现 Reasoning（调模型）→ Acting（执行工具）的交替循环，`maxIters(50)` 限制最大轮次。中间件可以拦截 `onReasoning` 和 `onActing` 两个阶段，但不能改变循环结构。`ContextTruncateMiddleware` 就是在 `onReasoning` 阶段截断上下文。

**babi-graph** 的 ReAct 循环由图结构定义。`AgentExecutor` 图有两个节点：agent 节点（调模型）和 action 节点（执行工具），通过条件边连接——模型返回 tool calls 时走 action 边，返回最终响应时走 END 边。`recursionLimit(50)` 限制图遍历深度。图结构的优势是可视化清晰，可以自定义节点和边扩展流程。

**babi-spring** 的 ReAct 循环由 `ToolCallingAdvisor` 在 Advisor 链中递归完成。流程是：发送 prompt 到模型（流式）→ 聚合流检测 tool calls → 执行工具 → 将工具结果加入消息历史 → 递归调用模型 → 过滤中间响应，只输出最终文本。这个循环对开发者透明——`ChatClient.stream().content()` 返回的 Flux 已经是过滤后的最终文本。`ToolCallObservationAdvisor` 可以观测每一轮调用，打印完整的消息历史和模型响应分析。

Advisor 链的递归设计有个优势——每一轮调用的消息历史完整可观测，因为 Advisor 能访问 `Prompt.getInstructions()` 获取累积的全部消息。babi-spring 的 `ToolCallObservationAdvisor` 正是利用这一点，通过统计 `ToolResponseMessage` 数量推断当前轮次，在每一轮打印完整的消息演进过程。相比之下，AgentScope 的中间件只能访问 `ReasoningInput.messages()`（当前轮的截断后消息），LangGraph4j 的 EdgeHook 只能访问 `State.lastMessage()`（最后一条消息），可观测性都不如 Spring AI。

三个模块的 ReAct 循环终止条件也不同。AgentScope 用 `maxIters(50)` 限制最大轮次；LangGraph4j 用 `recursionLimit(50)` 限制图遍历深度；Spring AI 的 `ToolCallingAdvisor` 没有显式的迭代限制，依赖模型的 finishReason 自然终止——如果模型不返回 tool calls，循环就结束。这意味着如果模型持续返回 tool calls，理论上可能无限循环。在实际使用中 Qwen 模型很少出现这种情况，但从工程健壮性角度，Spring AI 的方案缺少一道安全网。

## 八、Chat Memory：文件持久化 vs Checkpoint vs InMemory

三个框架的会话记忆机制差异很大。

**babi-agent** 用 `JsonFileAgentStateStore`，将对话状态持久化到 `~/.babi/sessions/` 目录下的 JSON 文件。重启后自动恢复，支持 CLI 模式的跨会话连续性。这是最重的持久化方案，但也是唯一支持离线恢复的。

**babi-graph** 用 LangGraph4j 的 `MemorySaver` 检查点机制，通过 `threadId` 隔离不同会话。检查点保存在内存中，重启丢失，但单次运行内支持多会话隔离。`BabiService.clearMemory()` 通过 `memorySaver.release(config)` 释放会话。

**babi-spring** 用 Spring AI 的 `MessageWindowChatMemory` + `InMemoryChatMemoryRepository`，设置 `maxMessages(50)` 的滑动窗口。这是最轻量的方案——内存存储、窗口截断、重启丢失。Spring AI 也支持 JDBC、Cassandra 等持久化 Repository，babi-spring 选择了最简单的内存方案。

三个模块都额外支持工作区目录下的 `MEMORY.md` 文件作为长期记忆补充，`Controller` 的 `DELETE /memory` 端点专门清理这个文件。

三种 Chat Memory 方案反映了不同的定位。babi-agent 的文件持久化是为 CLI 模式设计的——终端关闭后重开仍能延续对话，这对 Coding Agent 的用户体验至关重要。babi-graph 的检查点机制是为图编排服务的——可以在任意节点暂停和恢复，适合长时间运行的任务。babi-spring 的内存方案最简单，适合 Web 服务的单次会话场景。

但内存方案也有上下文膨胀的问题。babi-spring 的 `MessageWindowChatMemory` 设 `maxMessages(50)` 的滑动窗口，超过后自动丢弃最早的消息。这比 babi-agent 的 `ContextTruncateMiddleware`（30条窗口 + 保留 tool call/result 对）简单——后者在截断时会跳过孤立的 TOOL result 消息，避免模型看到没有对应请求的工具响应而困惑。Spring AI 的窗口截断没有这种语义保护，可能在截断时破坏 tool call/result 的配对完整性。

## 九、可观测设计：三种观测模式

**babi-agent** 依赖 AgentScope 框架的 `AgentTracingLog`，但 babi 显式关闭了它（`enableAgentTracingLog(false)`）以提升性能，改用中间件的 `log.debug` 做轻量观测。工具结果事件通过原生 `ToolResultEndEvent` 获取，无需自定义观测组件。

**babi-graph** 没有专门的可观测组件，依赖 `ToolNotificationEdgeHook` 的 `log.debug` 和 LangGraph4j 框架自身的图遍历日志。这是三个模块中可观测能力最弱的。

**babi-spring** 有最完善的自定义可观测——`ToolCallObservationAdvisor` 同时实现 `CallAdvisor` 和 `StreamAdvisor`，在每一轮 ReAct 循环中打印完整的消息历史、工具调用请求和响应、模型响应分析（是否包含 tool calls）、每轮耗时统计。通过统计 `ToolResponseMessage` 数量推断当前迭代轮次，输出格式化的日志：

```java
@Override
public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    List<Message> messages = request.prompt().getInstructions();
    long iteration = computeIteration(messages);
    log.info("║ [Advisor 链] 第 {} 轮调用（消息数: {}）", iteration, messages.size());
    logMessageHistory(messages);
    long startTime = System.currentTimeMillis();
    ChatClientResponse response = chain.nextCall(request);
    analyzeResponse(response, iteration, System.currentTimeMillis() - startTime);
    return response;
}
```

这个 Advisor 在调试 ReAct 循环问题时非常有效，能清晰看到每一轮模型决定调用哪些工具、工具返回了什么、模型何时终止循环。

## 十、Skill 加载能力：轻量文件 vs 企业级多源 vs 自研统一

Skill（可复用工作流指令）的加载能力是 Agent 工程化的重要一环。LangChain4j 和 AgentScope-Java 都内置了 Skill 系统，但设计哲学和实现深度差异显著。Spring AI 2.0 没有内置 Skill 机制，babi 自己实现了统一的 `SkillLoader`。

**LangChain4j：轻量级文件加载**

LangChain4j 从 1.12.1 版本开始正式引入 Agent Skills 支持，通过独立的 `langchain4j-skills` 模块提供（需额外引入依赖，截至 2026 年 4 月处于 beta 阶段）。采用 `SKILL.md` 文件定义技能，YAML frontmatter 声明元数据，Markdown 正文编写指令：

```java
List<FileSystemSkill> skills = ClassPathSkillLoader.loadSkills("skills");
Skills skillSet = Skills.from(skills);
// 注册到 AiServices
AiServices.builder(MyAgent.class)
    .toolProvider(skillSet.toolProvider())
    // ...
```

提供 `ClassPathSkillLoader` 从 classpath 加载、`FileSystemSkill` 从文件系统加载两种来源。支持通过 `agent.refreshSkills()` 热更新，无需重启。但加载策略是全量注入——Skill 内容在加载时一次性写入上下文，没有按需加载机制，在 Skill 数量多时会对上下文窗口造成压力。

**AgentScope-Java：企业级多源 Skill 系统**

AgentScope-Java 的 Skill 系统设计得更为完善，核心亮点是三层渐进式披露机制。元信息层：启动时仅将 Skill 的 description 注入 System Prompt，LLM 知道有哪些技能可用，但不加载完整内容。指令层：LLM 决定使用某个 Skill 后，通过内置 `read_skill`/`load_skill_through_path` 工具按需读取完整 `SKILL.md`。资源层：执行过程中按需加载 references 文档或执行 scripts 脚本。这种机制相比全量加载可节省约 85% 的上下文空间。

AgentScope-Java 2.0 还提供了丰富的 Repository 抽象支持多种来源加载：内置的 `WorkspaceSkillRepository`（本地 workspace 目录）和 `ClasspathSkillRepository`（打包为 jar），以及扩展模块的 `GitSkillRepository`（团队公共技能仓库）、`NacosSkillRepository`（配置中心动态管理）、`MysqlSkillRepository` 和 `PostgresSkillRepository`（数据库存储）。声明顺序即优先级，同名 Skill 以先声明的为准。此外，AgentScope 还将 Tool（MCP/Function Call）作为 Skill 的一种资源——Skill 未激活时绑定的 Tool 不会出现在工具列表中，激活后自动暴露，实现了 Tool 的渐进式披露。

**babi：自研统一 SkillLoader**

babi 没有直接使用 LangChain4j 或 AgentScope 的内置 Skill 系统，而是在 `babi-common` 中自研了统一的 `SkillLoader`。从三个目录按优先级加载——全局 `~/.agents/skills/`、Babi 专属 `~/.babi/skills/`、项目级 `{workspace}/.qoder/skills/`，后者覆盖前者。支持单文件（`my-skill.md`）和目录格式（`my-skill/SKILL.md`），YAML frontmatter 定义 name 和 description。

三个模块都通过自定义的 `SkillTool` 将 Skill 列表的 description 注入系统提示，并注册 `use_skill` 工具供 Agent 按需加载完整指令——当 Agent 调用 `use_skill("code-review")` 时，从内存取出对应 Skill 的完整 Markdown 指令体返回给模型。这其实是一种简化的渐进式披露：元信息层（description 注入提示）+ 指令层（按需加载完整内容），但没有第三层资源加载。babi-agent 通过 `disableDynamicSkills()` 显式关闭 AgentScope 的内置 Skill 系统，避免与自研 `SkillTool` 冲突。

**三者对比**

LangChain4j 的 Skill 加载够用但偏轻量，适合快速原型和简单场景。AgentScope-Java 的 Skill 系统更成熟，多源存储 + 渐进式披露的设计更适合企业级生产环境，尤其在 Skill 数量多、上下文窗口紧张的场景下优势明显。babi 的自研 `SkillLoader` 介于两者之间——比 LangChain4j 的全量加载多了按需加载，但不如 AgentScope 的三层渐进式完善。babi 选择自研而非直接用框架内置，原因有二：一是三个模块需要统一行为，不能只适配某个框架的 Skill API；二是需要完全控制 Skill 目录结构和加载逻辑，使其与 babi 的 workspace 概念深度集成。

## 总结

从工程化实践的角度看，三个框架各有定位。AgentScope 的 HarnessAgent 是"全家桶"——文件系统、状态持久化、Plan Mode、TaskList、企业级 Skill 系统开箱即用，适合快速搭建完整的 Agent 应用，但中间件扩展点有限。LangGraph4j 的图编排可视化清晰、检查点恢复能力强，适合复杂流程编排，但流式支持依赖手动桥接，Skill 系统仍在 beta 阶段。Spring AI 2.0 的 ChatClient + Advisor 链最灵活——每个关注点独立组装，可观测性最强，但需要自己实现 ChatModel 适配层和 Skill 机制。

babi 项目的工程化亮点在于"三模块共享同一套基础设施"——`babi-common` 的 Logic 层消除了工具实现差异，`CodingSystemPrompt` 统一了系统提示，`ToolEventBus` 统一了事件通知，`SessionContextHolder` 统一了会话传播。框架差异被隔离在 Agent 层和 ChatModel 层，对工具和前端完全透明。这种"公共能力下沉 + 框架差异隔离"的架构，是 AI Agent 多框架工程化落地的典范实践。

HTTP Client 层全选 JDK 原生、流式传输层因框架而异（Reactor/队列/Flowable）、ChatModel 层 AgentScope 最简洁而 Spring AI 控制力最强、Agent 层从重到轻依次为 HarnessAgent/CompiledGraph/ChatClient、工具定义层三种 `@Tool` 注解但共享 Logic——这些选型决策构成了 babi 工程化实践的全链路图景。
