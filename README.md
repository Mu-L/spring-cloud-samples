# ☁️ Spring Cloud Alibaba Samples
> 基于 **Spring Boot 4.1** + **Spring Cloud Alibaba 2025.1.x** 的生产级微服务示例项目 <br>
> 涵盖 16 个模块，覆盖 HTTP / Dubbo / gRPC / Stream 多协议通信及 Spring AI 多模态集成，支持一键演示与验证

![poster](poster.png)

### 🤖 AI 一键演示

> 本项目内置 Qoder Agent Skill，clone 后在 Qoder 中输入 `/demo-spring-cloud` 或告诉 AI "演示项目"，
> 即可自动完成环境检查、服务启动、接口验证全流程，无需手动操作。

```
# 快速体验（仅需 Nacos）
告诉 AI: "演示本项目"

# 单独验证某个场景
告诉 AI: "验证 Seata 分布式事务"
告诉 AI: "验证 Stream 消息收发"
告诉 AI: "演示 Spring AI"
告诉 AI: "演示一下视觉识别"
```

详见 [SKILL.md](.qoder/skills/demo-spring-cloud/SKILL.md)

### 📦 模块介绍
| 模块                               | 简称                | 端口           | 说明                          |
|----------------------------------|-------------------|--------------|-----------------------------|
| 🌐 cloud-gateway-sample          | gateway           | 8764         | Spring Cloud Gateway        |
| 📥 cloud-consumer-sample         | consumer          | 8766         | Web Consumer                |
| 📤 cloud-provider-sample         | provider          | 8765         | Web Provider                |
| ⚡ cloud-consumer-reactive-sample | consumer-reactive | 8763         | Reactive Web Consumer       |
| ⚡ cloud-provider-reactive-sample | provider-reactive | 8762         | Reactive Web Provider       |
| 🔗 cloud-provider-dubbo-sample   | provider-dubbo    | 50051        | Dubbo Provider              |
| 🔗 cloud-consumer-dubbo-sample   | consumer-dubbo    | -            | Dubbo Consumer              |
| 📋 cloud-sample-api              | api               | -            | Interface & Proto           |
| ⚙️ cloud-nacos-config-sample     | config            | 8761         | Nacos Config                |
| 🔍 cloud-nacos-discovery-sample  | discovery         | 8760         | Nacos Discovery             |
| 📨 cloud-stream-sample           | stream            | 8767         | Spring Cloud Stream         |
| 🔌 cloud-grpc-server-sample      | grpc-server       | 9090<br>8090 | gRPC Server<br>(8090是Web端口) |
| 🔌 cloud-grpc-client-sample      | grpc-client       | -            | gRPC Client                 |
| 🤖 cloud-ai-sample               | ai                | 8888         | Spring AI                   |
| 🔄 cloud-seata-sample            | seata             | -            | Apache Seata                |
| 🧩 cloud-commons                 | commons           | -            | Cloud Commons               |

<picture>
  <source srcset="arch.svg" type="image/svg+xml">
  <img src="arch.png" alt="架构图">
</picture>

### 🎮 演示方式

> **优先级：AI Skill > 脚本 > 手动**

| 方式 | 说明 | 适用场景 |
|------|------|----------|
| 🤖 **AI Skill（推荐）** | 告诉 AI 助手 "演示项目"，自动完成环境检查、启动、验证全流程 | 快速体验、集成测试 |
| 📜 **一键脚本** | 通过 `start-all.sh` 脚本自动化启动和验证 | 批量验证、CI/CD |
| 🔧 **手动启动** | 逐个模块手动启动，灵活控制 | 学习调试、单模块开发 |

#### 📜 一键脚本

```shell
# 查看所有命令
sh start-all.sh --help

# 常用命令
sh start-all.sh install  # 检查并安装中间件（Nacos/RocketMQ/MySQL/Seata）+ 打包模块
sh start-all.sh          # 启动所有服务（自动检查前置条件、打包、启动、验证）
sh start-all.sh build    # 打包所有模块
sh start-all.sh verify   # 执行验证（不启动，仅验证已运行的服务）
sh start-all.sh status   # 查看服务状态
sh start-all.sh logs <模块名>  # 查看模块日志（如 ai, stream, provider）
sh start-all.sh stop     # 停止所有服务（含 RocketMQ、Seata Server）
sh start-all.sh restart  # 重启所有服务
sh start-all.sh clean    # 清理构建产物
```

> 脚本流程：检查 Nacos → 检查 RocketMQ/MySQL/Seata Server（自动启动）→ 安装依赖模块 → 打包 → 按顺序启动所有模块 → 执行验证 → 汇总结果

### 🔍 服务注册与发现演示
> 首先安装部署 Nacos，完成后设置环境变量
```shell
export SPRING_CLOUD_NACOS_USERNAME=your_username
export SPRING_CLOUD_NACOS_PASSWORD=your_password
```

#### 🟢 Nacos Discovery 演示
启动discovery，访问如下接口
```shell
curl http://localhost:8760/discovery/instances
```

#### 🌐 普通 Web 服务的注册与发现
依次启动provider,consumer,gateway <br>
直接访问(consumer → provider)
```shell
curl 'http://localhost:8766/hi?name=hongxi'
```
通过网关访问(gateway → consumer → provider)
```shell
curl 'http://localhost:8764/consumer-sample/hi?name=hongxi'
```

#### ⚡ Reactive Web 服务注册与发现
接着启动provider-reactive,consumer-reactive <br>
直接访问(consumer-reactive → provider-reactive)
```shell
curl 'http://localhost:8763/hi?name=hongxi'
```
通过网关访问(gateway → consumer-reactive → provider-reactive)
```shell
curl 'http://localhost:8764/consumer-reactive-sample/hi?name=hongxi'
```

#### 🔗 Dubbo 服务注册与发现
接着启动provider-dubbo <br>
直接访问(consumer → provider-dubbo)
```shell
curl 'http://localhost:8766/dubbo?name=hongxi'
```
通过网关访问(gateway → consumer → provider-dubbo)
```shell
curl 'http://localhost:8764/consumer-sample/dubbo?name=hongxi'
```
直接访问(consumer-reactive → provider-dubbo)
```shell
curl 'http://localhost:8763/dubbo?name=hongxi'
```
通过网关访问(gateway → consumer-reactive → provider-dubbo)
```shell
curl 'http://localhost:8764/consumer-reactive-sample/dubbo?name=hongxi'
```

#### 🔌 gRPC 服务注册与发现
直接利用 Spring Cloud 的服务注册能力，引入`discovery`和`webmvc`依赖，<br>
同时，需要设置注册到注册中心的端口，否则默认注册的是`server.port`
```yaml
server:
  port: 8090 # Web端口
spring:
  cloud:
    nacos:
      discovery:
        port: ${spring.grpc.server.port} # 注册到注册中心的端口
  grpc:
    server:
      port: 9090 # gRPC端口
```
关于服务发现，Spring Cloud 与 gRPC 是两套服务发现模式，本项目使用 <br>
NameResolver SPI 桥接 DiscoveryClient 方式实现了两者服务发现模式的集成， <br>
具体实现请参考`cloud-commons`和`grpc-client-sample` <br>
接着前面的，启动grpc-server <br>
直接访问(consumer → grpc-server)
```shell
curl 'http://localhost:8766/grpc?name=hongxi'
```
通过网关访问(gateway → consumer → grpc-server)
```shell
curl 'http://localhost:8764/consumer-sample/grpc?name=hongxi'
```

#### 🎯 纯 Dubbo Provider/Consumer 演示
启动provider-dubbo,consumer-dubbo，观察日志

#### 🎯 纯 gRPC Server/Client 演示
启动grpc-server,grpc-client，观察日志

#### 🌐 Dubbo REST 演示
启动provider-dubbo,gateway <br>
直接访问`dubbo rest`接口
```shell
curl http://localhost:50051/api/hello/lily
curl 'http://localhost:50051/api/add?a=1&b=2'
curl -X POST http://localhost:50051/api/echo -H "Content-Type: application/json" -d '{"message":"hi"}'
curl 'http://localhost:50051/api/greet/lily?lang=zh'
```
通过网关访问`dubbo rest`接口(gateway → provider-dubbo)
```shell
curl http://localhost:8764/provider-dubbo-sample/api/hello/lily
curl 'http://localhost:8764/provider-dubbo-sample/api/add?a=1&b=2'
curl -X POST http://localhost:8764/provider-dubbo-sample/api/echo -H "Content-Type: application/json" -d '{"message":"hi"}'
curl 'http://localhost:8764/provider-dubbo-sample/api/greet/lily?lang=zh'
```

### 🔍 Trace 链路追踪

项目内置 trace 传播验证脚本，覆盖五条跨服务链路，验证 Spring Boot Observation 与各框架的 trace context 自动/手动传播：

| 链路 | 路径 | trace 传播 |
|------|------|------------|
| Web → Web | consumer → provider | RestTemplate / FeignClient 自动传播 |
| Web → gRPC | consumer → grpc-server | gRPC Interceptor 自动传播 |
| Web → Dubbo | consumer → provider-dubbo | Dubbo ObservationFilter 自动传播 |
| Reactive → Reactive | consumer-reactive → provider-reactive | WebClient 手动传递 traceparent |
| Reactive → Dubbo | consumer-reactive → provider-dubbo | Dubbo ObservationFilter 自动传播 |

```shell
bash .qoder/skills/demo-spring-cloud/verify-trace.sh
```

### ⚙️ Nacos Config 动态配置

启动 `cloud-nacos-config-sample`（端口 8761），通过模块提供的接口管理配置（避免直接调用 Nacos API 的鉴权问题）：
```shell
# 发布配置
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
# 获取配置
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
```

模块还演示了 `@NacosConfig`、`@ConfigurationProperties`、`@Value` + `@RefreshScope` 三种配置绑定方式，详细演示步骤参考 [SKILL.md](.qoder/skills/demo-spring-cloud/SKILL.md) 中的 Nacos Config 章节。

### 🛡️ Sentinel Gateway 演示
`cloud-gateway-sample`集成了sentinel，并采用nacos配置规则，规则示例如下 <br>
group-id: SENTINEL_GROUP <br>
data-id: cloud.sample.gateway.gw-api-group
```json
[
  {
    "apiName": "consumer_reactive_api",
    "predicateItems": [
      {
        "pattern": "/consumer-reactive-sample/**",
        "matchStrategy": 1
      }
    ]
  },
  {
    "apiName": "consumer_api",
    "predicateItems": [
      {
        "pattern": "/consumer-sample/**",
        "matchStrategy": 1
      }
    ]
  }
]
```
group-id: SENTINEL_GROUP <br>
data-id: cloud.sample.gateway.gw-flow
```json
[
  {
    "resource": "consumer_reactive_api",
    "resourceMode": 1,
    "count": 10
  },
  {
    "resource": "consumer_api",
    "resourceMode": 1,
    "count": 5
  },
  {
    "resource": "consumer-reactive-sample",
    "resourceMode": 0,
    "count": 20
  }
]
```
演示：在浏览器快速刷新访问几次如下接口
```text
http://localhost:8764/consumer-sample/hi?name=hongxi
```
触发限流时返回
```json
{"code":444,"msg":"Sentinel gateway block"}
```

### 📨 Stream 演示

演示 Spring Cloud Stream 的六大核心场景：

| 场景 | 函数类型 | 消息流 | 说明 |
|------|----------|--------|------|
| 基础消费 | Consumer | StreamBridge → topic → input | 启动时自动发送 "Hello" 并消费 |
| 定时消息源 | Supplier | output2 → topic2 → input2 | 每隔1秒自动发送 "你好" |
| 消息处理管道 | Function | REST → transform → [toUpperCase] → topic2 | 消息转换后输出 |
| 延迟消息 | Consumer | StreamBridge → delay-topic → delay | 通过 DELAY header 指定延迟级别后延迟投递 |
| 顺序消息 | Consumer | StreamBridge → fifo-topic → fifo | 相同 orderKey 保证顺序消费 |
| 事务消息 | Consumer | StreamBridge → tx-topic → tx | 两阶段提交，随机模拟本地事务成功/失败 |

#### 🏃 Run RocketMQ locally
download [rocketmq-all-5.5.0-bin-release.zip](https://dist.apache.org/repos/dist/release/rocketmq/5.5.0/rocketmq-all-5.5.0-bin-release.zip)
```shell
bin/mqnamesrv
bin/mqbroker -n localhost:9876
```

#### 📝 Create Topic and Consumer Group
```shell
# 基础消息
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-demo-consumer-group
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic2 -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-demo-consumer-group2
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-transform-topic -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-transform-group
# 延迟消息（DELAY 类型）
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-delay-topic -a +message.type=DELAY
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-delay-group
# 顺序消息（FIFO 类型）
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-fifo-topic -a +message.type=FIFO
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-fifo-group
# 事务消息（TRANSACTION 类型）
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-tx-topic -a +message.type=TRANSACTION
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-tx-group
```

#### 🏃 Run Demo
启动`stream`，观察日志（基础消费 + 定时消息源自动触发）

场景1、2 启动后自动触发，观察日志即可。场景2 验证后通过 Actuator 端点停止定时消息源，避免后续场景日志刷屏：
```shell
curl -s -X POST "http://localhost:8767/actuator/bindings/output2-out-0" -H "Content-Type: application/json" -d '{"state":"STOPPED"}'
```

继续通过 REST API 交互式验证各场景：
```shell
# 场景3: 消息处理管道 - 发送消息到 transform 函数（观察大写转换）
curl -X POST "http://localhost:8767/stream/send?message=hello+spring+cloud"
# 日志观察: 消息转换: hello spring cloud -> [PROCESSED] HELLO SPRING CLOUD

# 场景4: 延迟消息 - 发送延迟消息（delayLevel=2 即 5秒后投递）
curl -X POST "http://localhost:8767/stream/delay?message=hello+delay&delayLevel=2"
# 日志观察: [延迟消息] 收到: hello delay (时间: ...) — 注意接收时间与发送时间差约5秒

# 场景5: 顺序消息 - 发送带相同 orderKey 的消息（保证顺序消费）
curl -X POST "http://localhost:8767/stream/fifo?message=order-1&orderKey=order-A"
curl -X POST "http://localhost:8767/stream/fifo?message=order-2&orderKey=order-A"
curl -X POST "http://localhost:8767/stream/fifo?message=order-3&orderKey=order-A"
# 日志观察: [顺序消息] 收到消息按发送顺序依次被消费

# 场景6: 事务消息 - 发送事务消息（两阶段提交，随机决定提交或回滚）
curl -X POST "http://localhost:8767/stream/tx?message=hello+tx"
# 日志观察: [事务消息] 执行本地事务 → [事务消息] 本地事务提交 (随机) 或 本地事务回滚 (随机)
# 多次调用可观察到 commit 和 rollback 两种场景
```

查看消费组的消费进度：
```shell
bin/mqadmin consumerProgress -n localhost:9876 -g stream-demo-consumer-group2
```

### 🔄 Seata 分布式事务演示

前置条件：MySQL + Seata Server，请参考 [seata-sample/README](cloud-seata-sample/README.md) 中的环境准备和运行示例。

启动 4 个微服务（business 18081、storage 18082、order 18083、account 18084），验证分布式事务的回滚与提交。

### 🤖 Spring AI 演示

基于 **Spring AI 2.0**，集成阿里云百炼（DashScope）兼容 OpenAI 协议。

前置条件：配置 API Key
```shell
export OPENAI_API_KEY=your-api-key-here
```

启动 AI 模块（端口 8888），默认使用 `qwen-plus` 纯文本模型，视觉识别接口自动切换为 `qwen3.7-plus` 多模态模型。

#### 基础能力

| 接口 | 说明 | 示例 |
|------|------|------|
| `/ai/chat` | 简单聊天 | `curl --get --data-urlencode "message=你好" "http://localhost:8888/ai/chat"` |
| `/ai/chat/stream` | 流式输出（SSE） | `curl --get --data-urlencode "message=讲一个故事" "http://localhost:8888/ai/chat/stream"` |
| `/ai/extract` | 结构化输出 | `curl --get --data-urlencode "message=张三今年25岁，是软件工程师" "http://localhost:8888/ai/extract"` |

#### 高级对话

| 接口 | 说明 |
|------|------|
| `/ai/advanced/system-message` | System Message 设定 AI 角色 |
| `/ai/advanced/few-shot` | Few-shot Prompting 示例引导 |
| `/ai/advanced/conversation` | 多轮对话（连续发送，AI 记住上下文） |
| `/ai/advanced/creative` | 带温度参数的创意性对话 |

#### Tool Calling & Agent

| 接口 | 说明 |
|------|------|
| `/ai/tool/weather` | 天气查询（AI 自动调用 WeatherTools） |
| `/ai/tool/time` | 时间查询（AI 自动调用 TimeTools） |
| `/ai/tool/smart-assistant` | 智能助手（自动选择合适的工具） |
| `/ai/agent/chat` | ReAct Agent（多步推理 + 工具组合） |
| `/ai/demo` | 项目演示 Agent（自主调用工具验证本项目） |

#### MCP Server

通过 SSE 端点 `http://localhost:8888/sse` 暴露工具，支持跨进程 Agent 通信。

#### 多模态视觉识别

| 接口 | 说明 |
|------|------|
| `/ai/vision/analyze-url` | URL 图片分析 |
| `/ai/vision/analyze-upload` | 上传图片分析 |
| `/ai/vision/ocr` | OCR 文字识别 |
| `/ai/vision/chart-analysis` | 图表分析 |
| `/ai/vision/code-from-image` | 代码截图转代码 |
| `/ai/vision/compare` | 多图片对比 |

#### DeepSeek 多提供商集成

同一模块内集成 DashScope + DeepSeek 两个提供商，验证 Spring AI 的多模型管理能力。需额外配置 `export DEEPSEEK_API_KEY=your-key`。

| 接口 | 说明 |
|------|------|
| `/deepseek/chat` | 简单聊天 |
| `/deepseek/chat/stream` | 流式输出 |
| `/deepseek/system-message` | System Message 设定角色 |
| `/deepseek/creative` | 创意性对话 |
| `/deepseek/agent/chat` | ReAct Agent |

> 完整的 curl 命令示例和验证流程请参考 [SKILL.md](.qoder/skills/demo-spring-cloud/SKILL.md) 中的 Spring AI 章节。

### 🌿 分支说明
- 🌱 `springboot3`: 基于 Spring Boot 3.5.0+ 的示例
- 🌿 `eureka`: 初始版本，使用 Eureka 作为注册中心

&copy; [hongxi.org](http://hongxi.org)
