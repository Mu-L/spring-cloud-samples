---
name: demo-spring-cloud
description: >
  启动和演示 Spring Cloud Alibaba 示例项目的各微服务模块。当用户要求演示项目、启动服务、
  验证微服务调用、测试网关路由、查看服务注册、执行集成测试、一键部署、环境检查、
  排查微服务问题、了解 Spring Cloud 组件用法、学习 Nacos/Sentinel/Seata/Dubbo/gRPC/Stream/Kafka 时
  使用此技能。也支持演示特定功能：ChatMemory 多轮对话记忆、PromptTemplate 提示词模板、
  RAG 检索增强生成、Spring AI 视觉识别、Tool Calling、ReAct Agent、DeepSeek 集成、
  Trace 链路追踪、Nacos Config 动态配置、Sentinel 限流熔断、Stream 消息收发、Seata 分布式事务、
  Kafka 4.x 集群消息收发。
  涵盖 16 个模块的完整演示流程。
tags: [spring-cloud, spring-cloud-alibaba, nacos, sentinel, seata, dubbo, grpc, rocketmq, stream, kafka, microservices, demo, spring-ai, rag, chatmemory, prompttemplate, vision, tool-calling, agent, trace]
---

# Spring Cloud Alibaba 示例项目演示

## ⚠️ 重要说明

**所有验证操作必须严格按照本 SKILL 的要求执行，特别是：**

1. **Nacos 配置管理**：读写 Nacos 配置时，**必须使用 `cloud-nacos-config-sample` 模块提供的接口**（端口 8761），不要直接使用 Nacos 官方 HTTP API。
   - ✅ 正确方式：`http://localhost:8761/nacos/publishConfig`、`http://localhost:8761/nacos/getConfig`
   - ❌ 错误方式：`http://localhost:8080/nacos/v1/cs/configs`

2. **验证流程**：按照 SKILL 中定义的步骤顺序执行，不要跳过任何前置检查或验证环节。

3. **端口规范**：项目已统一端口分配，AI 模块使用 8888 端口，禁止使用 8080 端口。

4. **脚本输出规范**：执行任何项目脚本（`start-all.sh`、`verify-*.sh`）时，**必须完整输出脚本的全部日志**，禁止使用 `tail -n`、`tail -f`、`head -n` 等方式截断输出。脚本的完整输出是验证结果的依据，截断会导致无法确认每一步是否通过。

---

## 项目介绍

**GitHub**：https://github.com/javahongxi/spring-cloud-samples

基于 **Spring Boot 4.x + Spring Cloud Alibaba 2025.1.x** 的生产级微服务示例项目，涵盖服务发现、配置管理、网关路由、链路追踪、限流熔断、分布式事务、消息驱动、Spring AI 等核心能力，包含 16 个模块，支持 AI 一键演示。

## 30 秒快速体验

只需两步：
1. 克隆项目：`git clone https://github.com/javahongxi/spring-cloud-samples.git`
2. 确保 Nacos 已运行（没有？告诉 AI "安装 Nacos"）
3. 告诉 AI **"演示本项目"**

AI 会自动完成：环境检查 → 依赖安装 → 服务启动 → 接口验证 → 结果汇总。无需手动操作。

> 也可以只验证单个模块，例如："验证 Seata 分布式事务"、"验证 Stream 消息收发"、"演示 Spring AI"、"演示 Kafka 消息收发"

### 验证层级说明

**基础验证**（`sh start-all.sh` 自动执行）：
- ✓ 服务注册发现
- ✓ 健康检查
- ✓ 基础调用链路（Web/Reactive/Dubbo/gRPC）
- ✓ 网关路由
- ✓ Nacos Config 健康检查
- ✓ AI/Stream/Seata/Kafka 模块健康检查

**深度验证**（需要单独执行）：
- 🔍 Trace 链路追踪：五条链路（Web→Web / Web→gRPC / Web→Dubbo / Reactive→Reactive / Reactive→Dubbo）trace ID 自动传播、跨服务 trace 上下文传递
- 🔍 Nacos Config 动态配置：配置发布/读取/删除、@NacosConfig 注解、@ConfigurationProperties + @Value 动态刷新
- 🔍 Sentinel 限流：网关限流规则、应用级接口限流与 Feign/RestTemplate 熔断降级
- 🔍 Stream 消息消费：StreamBridge 编程式发布、Supplier 定时消息源、Function 消息处理管道
- 🔍 Seata 分布式事务：全局事务回滚/提交、Xid 传递、Dubbo 链路事务、数据一致性
- 🔍 Spring AI 深度功能：聊天对话、流式输出、Tool Calling、ReAct Agent、多模态视觉识别、ChatMemory 多轮对话记忆、PromptTemplate 提示词模板
- 🔍 Spring AI RAG 模块：RAG 检索增强生成全流程
- 🔍 Kafka 4.x 集群消息收发：3 节点 KRaft 集群部署、Producer/Consumer 消息收发

> 使用 `demo-spring-cloud` skill 可执行深度验证脚本（verify-trace.sh / verify-stream.sh / verify-seata.sh），或查看 [references/](references/) 目录下的各模块详细文档

## 前置条件

### 1. Nacos 注册中心（必须）

所有模块依赖 Nacos，启动前先确认 Nacos 已就绪。

**当用户说"安装 Nacos"时，按以下流程执行：**

**Step 1：检查 Nacos 状态**
```bash
curl -s http://127.0.0.1:8848/nacos/actuator/health | grep -q '"status":"UP"' && echo "✓ Nacos 已运行" || echo "✗ Nacos 未运行"
```

根据检查结果处理：

**已运行 ✓** → 跳到 Step 4 设置环境变量

**已安装但未运行** → 查找并启动：
```bash
NACOS_DIR=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
cd "$NACOS_DIR" && bin/startup.sh -m standalone
```
启动完成后跳到 Step 4。

**未安装** → 一键安装并部署：
```bash
curl -fsSL https://nacos.io/nacos-installer.sh | bash
nacos-setup  # 本地一键部署单机版 Nacos
```
> nacos-setup 自动下载安装、生成鉴权配置、检测端口冲突和 Java 环境。部署后自动创建账号（用户名：nacos），密码是无规律字符串。首次部署后会自动打开浏览器 http://127.0.0.1:8080/ 登录 Console。跳到 Step 4。

**Step 4：设置环境变量**
提示用户设置环境变量（用户名/密码为安装时创建的凭证）：
```bash
export SPRING_CLOUD_NACOS_USERNAME=nacos
export SPRING_CLOUD_NACOS_PASSWORD=<安装时设置的密码>
```

**Step 5：验证**
等待 Nacos 启动完成后再次检查健康状态，确认 `"status":"UP"` 后告知用户 Nacos 已就绪。

**停止 Nacos**：
```bash
bin/shutdown.sh
```

### 2. RocketMQ（仅 Stream 模块需要）

Stream 模块依赖 RocketMQ，安装和启动步骤参考 [stream.md](references/stream.md)。

### 3. MySQL + Seata Server（仅 Seata 模块需要）

Seata 分布式事务依赖 MySQL 和 Seata Server，安装和启动步骤参考 [seata.md](references/seata.md)。

### 4. Kafka 4.x 集群（仅 Kafka 模块需要）

Kafka 消息收发依赖 Kafka 4.x 集群（KRaft 模式），部署步骤参考 [kafka.md](references/kafka.md)。

### 5. 安装依赖模块

部分模块依赖 `cloud-commons` 和 `cloud-sample-api`，启动前需先安装：
```bash
./mvnw -N install -q && ./mvnw -pl cloud-commons,cloud-sample-api install -DskipTests -q
```

## 启动方式

### 一键脚本

```bash
sh start-all.sh install  # 检查并安装中间件 + 打包模块
sh start-all.sh          # 启动所有服务（自动检查前置条件、打包、启动、验证）
sh start-all.sh seata    # 仅启动 Seata 分布式事务 (7个模块)
sh start-all.sh build    # 打包所有模块
sh start-all.sh verify   # 执行验证（不启动，仅验证已运行的服务）
sh start-all.sh status   # 查看服务状态
sh start-all.sh logs <模块名>  # 查看模块日志（如 ai, stream, provider）
sh start-all.sh stop     # 停止所有服务（含 RocketMQ、Seata Server）
```

> 脚本流程：检查 Nacos → 检查 RocketMQ/MySQL/Seata Server（自动启动）→ 安装依赖模块 → 打包 → 按顺序启动所有模块 → 执行验证 → 汇总结果

### 手动逐个启动

启动顺序：**基础设施 → Config → Gateway → Provider → Consumer**

```bash
./mvnw -pl <模块目录> spring-boot:run
```

**核心模块（按顺序）：**

| 模块                             | 端口                   | 说明                |
|--------------------------------|----------------------|-------------------|
| cloud-nacos-discovery-sample   | 8760                 | 服务发现              |
| cloud-nacos-config-sample      | 8761                 | Nacos Config      |
| cloud-gateway-sample           | 8764                 | 网关                |
| cloud-provider-sample          | 8765                 | Web Provider      |
| cloud-provider-reactive-sample | 8762                 | Reactive Provider |
| cloud-provider-dubbo-sample    | 50051                | Dubbo Provider    |
| cloud-grpc-server-sample       | 8090(Web)/9090(gRPC) | gRPC Server       |
| cloud-consumer-sample          | 8766                 | Web Consumer      |
| cloud-consumer-reactive-sample | 8763                 | Reactive Consumer |

**独立模块（无启动顺序依赖）：**

| 模块                  | 端口                    | 说明                                                       |
|---------------------|-----------------------|----------------------------------------------------------|
| cloud-ai-sample     | 8888                  | Spring AI，需配置 OPENAI_API_KEY                             |
| cloud-ai-rag-sample | 8889                  | Spring AI · RAG，需 PostgreSQL + pgvector + OPENAI_API_KEY |
| cloud-stream-sample | 8767                  | 需先安装并启动 RocketMQ                                         |
| cloud-seata-sample  | 18081-18084 + 3 Dubbo | 需 MySQL + Seata Server，含 7 个子模块                          |
| cloud-kafka-sample  | 8768                  | 需 Kafka 4.x 集群（KRaft 模式）                                 |

## 演示与验证

> 各场景的详细演示步骤和 curl 命令已整合到 [references/](references/) 目录下的对应文档中，以下列出各场景的入口和参考链接。

### 1. 服务注册与发现（Web / Reactive / Dubbo / gRPC）

参考 [discovery.md](references/discovery.md)

### 2. Trace 链路追踪

```bash
bash .qoder/skills/demo-spring-cloud/scripts/verify-trace.sh
```

参考 [trace.md](references/trace.md)

### 3. Nacos Config 动态配置

参考 [nacos-config.md](references/nacos-config.md)

### 4. Sentinel 网关限流

参考 [sentinel-gateway.md](references/sentinel-gateway.md)

### 5. Sentinel 应用级熔断降级

参考 [sentinel-app.md](references/sentinel-app.md)

### 6. Stream 消息收发（需 RocketMQ）

> 本模块演示 Spring Cloud Stream 六大核心场景：基础消费、定时消息源、消息处理管道、延迟消息、顺序消息、事务消息。

**执行流程：**

1. **检查 RocketMQ 并询问用户**：
   ```bash
   nc -z 127.0.0.1 9876 2>/dev/null && echo "✓ RocketMQ 已运行" || echo "✗ RocketMQ 未运行"
   ```

2. **询问用户是否需要 AI 自动完成环境准备**：
   > "Stream 模块依赖 RocketMQ，但当前未检测到运行中的 RocketMQ 服务。是否需要我帮您自动完成以下操作？
   > 1. 启动 NameServer 和 Broker
   > 2. 创建所需的 Topic 和 Consumer Group
   > 3. 启动 Stream 模块并验证消息收发"

   **如果用户同意**，直接执行一键验证脚本：
   ```bash
   bash .qoder/skills/demo-spring-cloud/scripts/verify-stream.sh
   ```

   详细手动操作步骤参考 [stream.md](references/stream.md)

### 7. Seata 分布式事务（需 MySQL + Seata Server）

**执行流程：**

1. **检查前置条件**：
   ```bash
   # 检查 Nacos 是否运行
   if curl -s -o /dev/null -w '' "http://127.0.0.1:8848/nacos/actuator/health" 2>/dev/null; then
     echo "✓ Nacos 已运行"
   else
     echo "✗ Nacos 未运行，请先启动 Nacos"
     return 1
   fi

   # 检查 MySQL 是否运行
   if mysql -u root -proot1234 -e "SELECT 1" &>/dev/null; then
     echo "✓ MySQL 已运行"
   else
     echo "✗ MySQL 未运行或连接失败"
     return 1
   fi
   ```

2. **询问用户是否需要 AI 自动完成环境准备**：
   > "Seata 分布式事务示例需要以下环境：
   > 1. MySQL 数据库（已检测到运行中 / 未检测到）
   > 2. 初始化 seata 数据库及业务表
   > 3. 配置 Nacos（创建 seata.properties）
   > 4. 启动 Seata Server
   > 5. 启动 7 个微服务并验证分布式事务
   > 
   > 是否需要我帮您自动完成以上操作？"

   **如果用户同意**，直接执行一键验证脚本：
   ```bash
   bash .qoder/skills/demo-spring-cloud/scripts/verify-seata.sh
   ```

   详细手动操作步骤参考 [seata.md](references/seata.md)

### 8. Spring AI 模块

> ⏱️ AI 接口调用大模型 API，每次响应通常需 **5~30 秒**。建议所有 curl 命令加 `--max-time 60`。

参考 [spring-ai.md](references/spring-ai.md)

### 9. Spring AI RAG 模块（需 PostgreSQL + pgvector）

> ⏱️ AI 接口调用大模型 API，每次响应通常需 **5~30 秒**。建议所有 curl 命令加 `--max-time 60`。

参考 [spring-ai-rag.md](references/spring-ai-rag.md)

### 10. Kafka 4.x 消息收发（需 Kafka 集群）

> Kafka 集群需单独部署，详见 [kafka.md](references/kafka.md) 中的集群部署章节。

参考 [kafka.md](references/kafka.md)

## 常见问题

| 问题                     | 解决方案                                                                                             |
|------------------------|--------------------------------------------------------------------------------------------------|
| 服务注册失败                 | 检查 Nacos 是否启动，环境变量是否设置                                                                           |
| 端口冲突                   | 检查端口是否被占用，或修改 application.yml 中的端口                                                               |
| gRPC 调用失败              | 确认 grpc-server 的 gRPC 端口 9090 可访问                                                                |
| Sentinel 未限流           | 检查 Nacos 中是否配置了对应的 Sentinel 规则                                                                   |
| AI 模块 401              | 检查 OPENAI_API_KEY 是否正确配置                                                                         |
| AI 视觉识别 500            | 图片 URL 不可访问（百度图片会拒绝 Java UrlResource 请求），使用稳定可访问的 URL                                            |
| AI 接口 400              | 中文参数需 URL 编码，使用 `--get --data-urlencode`                                                         |
| RAG 模块连接 PostgreSQL 失败 | 确认 PostgreSQL 已运行（`pg_isready`），已执行 `init_ai_demo.sql` 初始化数据库                                    |
| RAG 摄入返回 0 chunks      | 检查 content 是否为空，确认 PgVector 扩展已启用（`\connect ai_demo` 后 `CREATE EXTENSION vector`）                |
| RAG 查询回答未引用参考资料        | 确认文档已成功摄入（ingest 返回 chunks > 0），检查 topK 参数是否合理                                                   |
| ChatMemory 无记忆效果       | 确认 JDBC 表 `SPRING_AI_CHAT_MEMORY` 已自动创建（ai 模块端口 8888），检查 conversationId 是否一致                     |
| Kafka 模块连接失败           | 确认 Kafka 集群已启动（端口 9092/9094/9096），已创建 share-demo-topic、share-demo-topic-explicit 和 tx-demo-topic |
| Stream 发送消息报超时异常       | 重启 Broker                                                                                        |
