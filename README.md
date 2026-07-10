# ☁️ Spring Cloud Alibaba Samples
> 基于 **Spring Boot 4.1** + **Spring Cloud Alibaba 2025.1.x** 的生产级微服务示例项目 <br>
> 涵盖 16 个模块，覆盖 HTTP / Dubbo / gRPC / Stream / Kafka 多协议通信与消息驱动、Spring AI 多模态集成及 Seata 分布式事务，支持一键演示与验证

![poster](poster.png)

### 📦 模块介绍
| 模块                               | 简称                | 端口    | 说明                    |
|----------------------------------|-------------------|-------|-----------------------|
| 🔍 cloud-nacos-discovery-sample  | discovery         | 8760  | Nacos Discovery       |
| ⚙️ cloud-nacos-config-sample     | config            | 8761  | Nacos Config          |
| ⚡ cloud-provider-reactive-sample | provider-reactive | 8762  | Reactive Web Provider |
| ⚡ cloud-consumer-reactive-sample | consumer-reactive | 8763  | Reactive Web Consumer |
| 🌐 cloud-gateway-sample          | gateway           | 8764  | Spring Cloud Gateway  |
| 📤 cloud-provider-sample         | provider          | 8765  | Web Provider          |
| 📥 cloud-consumer-sample         | consumer          | 8766  | Web Consumer          |
| 🚀 cloud-provider-dubbo-sample   | provider-dubbo    | 50051 | Dubbo Provider        |
| 🔌 cloud-grpc-server-sample      | grpc-server       | 9090  | gRPC Server           |
| 📋 cloud-sample-api              | api               | -     | Interface & Proto     |
| 🧩 cloud-commons                 | commons           | -     | Cloud Commons         |
| 📨 cloud-stream-sample           | stream            | 8767  | Spring Cloud Stream   |
| 🔄 cloud-seata-sample            | seata             | -     | Seata (含 7 个子模块)      |
| 🤖 cloud-ai-sample               | ai                | 8888  | Spring AI             |
| 🤖 cloud-ai-rag-sample           | rag               | 8889  | Spring AI · RAG       |
| 📨 cloud-kafka-sample            | kafka             | 8768  | Kafka 4.x             |

<picture>
  <source srcset="arch.svg" type="image/svg+xml">
  <img src="arch.png" alt="架构图">
</picture>

### 🎮 演示方式

| 方式                  | 说明                                | 适用场景       |
|---------------------|-----------------------------------|------------|
| 🤖 **AI Skill（推荐）** | 告诉 AI 助手 "演示项目"，自动完成环境检查、启动、验证全流程 | 快速体验、集成测试  |
| 📜 **一键脚本**         | 通过 `start-all.sh` 脚本自动化启动和验证      | 批量验证、CI/CD |
| 🐳 **Docker 部署**    | 中间件本地运行，微服务全部容器化                  | 容器化实践、贴近生产 |
| 🔧 **手动启动**         | 逐个模块手动启动，灵活控制                     | 学习调试、单模块开发 |

#### 🤖 AI 一键演示（推荐）

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

#### 📜 一键脚本

```shell
sh start-all.sh install  # 检查并安装中间件 + 打包模块
sh start-all.sh          # 启动所有服务（自动检查、打包、启动、验证）
sh start-all.sh build    # 打包所有模块
sh start-all.sh stop     # 停止所有服务
```

> 更多命令（seata / verify / status / logs 等）请参考 `sh start-all.sh --help`
>
> 脚本流程：检查 Nacos → 检查 RocketMQ/MySQL/Seata Server（自动启动）→ 安装依赖模块 → 打包 → 按顺序启动所有模块 → 执行验证 → 汇总结果

#### 🐳 Docker 部署

中间件本地运行，微服务全部 Docker 容器化，通过 `host.docker.internal` 连接宿主机中间件。

**架构**

```
Mac 宿主机
├── 本地中间件: Nacos(8848) / RocketMQ(9876) / MySQL(3306) / PostgreSQL(5432)
│
└── Docker 容器 (通过 host.docker.internal 连宿主机)
    ├── 核心微服务 (9个): gateway / consumer / provider / grpc-server ...
    ├── Stream 消息         (profile: stream)
    ├── Spring AI           (profile: ai)
    └── Seata 分布式事务   (profile: seata)
```

**快速开始**

```shell
# 1. 启动本地中间件（Nacos / RocketMQ / MySQL / PostgreSQL）
./start-all.sh infra

# 2. Maven 打包 + 构建所有 Docker 镜像
./docker-build.sh build

# 3. 启动核心微服务 (9个)
./docker-build.sh up

# 4. 验证
curl 'http://localhost:8766/hi?name=docker'
curl 'http://localhost:8764/consumer-sample/hi?name=docker'
```

**常用命令**

```shell
./docker-build.sh build   # Maven 打包 + 构建所有 Docker 镜像
./docker-build.sh up      # 启动核心微服务 (9个)
./docker-build.sh up-all  # 启动全部 (含 Stream/AI/Seata)
./docker-build.sh down    # 停止所有微服务
```

> 更多命令（build-one / up-stream / up-ai / up-seata / status / logs / clean 等）请参考 `./docker-build.sh --help`
>
> Docker 需要 [OrbStack](https://orbstack.dev)（`brew install orbstack`），国内拉镜像需配置[镜像加速](https://docs.orbstack.dev/docker/registry-mirrors)。

#### 🔧 手动启动

按下面的功能演示章节逐步操作即可，每个章节都包含前置条件、启动顺序和验证命令。
详细演示步骤请参考 [references/](.qoder/skills/demo-spring-cloud/references/) 目录下的文档。

### 🔍 服务注册与发现演示

覆盖 Nacos Discovery、Web / Reactive / Dubbo / gRPC 多协议服务注册与发现。

> 详细演示步骤参考 [discovery.md](.qoder/skills/demo-spring-cloud/references/discovery.md)

### 🔍 Trace 链路追踪

覆盖五条跨服务链路（Web→Web / Web→gRPC / Web→Dubbo / Reactive→Reactive / Reactive→Dubbo），验证 Spring Boot Observation 的 trace context 传播。

> 详细演示步骤参考 [trace.md](.qoder/skills/demo-spring-cloud/references/trace.md)

### ⚙️ Nacos Config 动态配置

演示 `@NacosConfig`、`@ConfigurationProperties`、`@Value` + `@RefreshScope` 三种配置绑定方式。

> 详细演示步骤参考 [nacos-config.md](.qoder/skills/demo-spring-cloud/references/nacos-config.md)

### 🛡️ Sentinel 限流熔断

Gateway 级限流和应用级熔断降级（Feign / RestTemplate），规则通过 Nacos 动态推送。

> 详细演示步骤参考 [sentinel-gateway.md](.qoder/skills/demo-spring-cloud/references/sentinel-gateway.md) 和 [sentinel-app.md](.qoder/skills/demo-spring-cloud/references/sentinel-app.md)

### 📨 Stream 消息驱动演示

Spring Cloud Stream 六大核心场景：基础消费、定时消息源、消息处理管道、延迟消息、顺序消息、事务消息。

> 详细演示步骤参考 [stream.md](.qoder/skills/demo-spring-cloud/references/stream.md)

### 🔄 Seata 分布式事务演示

7 个子模块，支持 RestTemplate / Feign / Dubbo 三种调用链路，演示分布式事务的提交与回滚。

> 详细演示步骤参考 [seata.md](.qoder/skills/demo-spring-cloud/references/seata.md)

### 🤖 Spring AI 演示

基于 Spring AI 2.0，涵盖基础对话、高级对话、Tool Calling、ReAct Agent、多模态视觉识别、DeepSeek 多提供商集成、ChatMemory 多轮对话记忆、PromptTemplate 提示词模板。

> 详细演示步骤参考 [spring-ai.md](.qoder/skills/demo-spring-cloud/references/spring-ai.md)

### 🤖 Spring AI RAG 演示

基于 Spring AI 2.0 的检索增强生成，支持 PgVector 和 Redis (RediSearch) 两种向量存储，通过 Profile 一键切换。

> 详细演示步骤参考 [spring-ai-rag.md](.qoder/skills/demo-spring-cloud/references/spring-ai-rag.md)

### 📨 Kafka 4.x 消息收发演示

基于 Kafka 4.x KRaft 模式，演示传统 Consumer Group、Share Groups（并行消费 + 逐条确认）以及事务消息。

> 详细演示步骤参考 [kafka.md](.qoder/skills/demo-spring-cloud/references/kafka.md)

### 🌿 分支说明
- 🌱 `springboot3`: 基于 Spring Boot 3.5.0+ 的示例
- 🌿 `eureka`: 初始版本，使用 Eureka 作为注册中心

&copy; [hongxi.org](http://hongxi.org)
