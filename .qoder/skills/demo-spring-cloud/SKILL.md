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
  涵盖 17 个模块的完整演示流程。
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

## 30 秒快速体验

只需两步：
1. 确保 Nacos 已运行（没有？告诉 AI "安装 Nacos"）
2. 告诉 AI **"演示本项目"**

AI 会自动完成：环境检查 → 依赖安装 → 服务启动 → 接口验证 → 结果汇总。无需手动操作。

> 也可以只验证单个模块，例如："验证 Seata 分布式事务"、"验证 Stream 消息收发"、"演示 Spring AI"

### 验证层级说明

**基础验证**（`sh start-all.sh` 自动执行）：
- ✓ 服务注册发现
- ✓ 健康检查
- ✓ 基础调用链路（Web/Reactive/Dubbo/gRPC）
- ✓ 网关路由
- ✓ Nacos Config 健康检查
- ✓ AI/Stream/Seata 模块健康检查

**深度验证**（需要单独执行）：
- 🔍 Trace 链路追踪：五条链路（Web→Web / Web→gRPC / Web→Dubbo / Reactive→Reactive / Reactive→Dubbo）trace ID 自动传播、跨服务 trace 上下文传递
- 🔍 Nacos Config 动态配置：配置发布/读取/删除、@NacosConfig 注解、@ConfigurationProperties + @Value 动态刷新
- 🔍 Sentinel 限流：网关限流规则、应用级接口限流与 Feign/RestTemplate 熔断降级
- 🔍 Stream 消息消费：StreamBridge 编程式发布、Supplier 定时消息源、Function 消息处理管道
- 🔍 Seata 分布式事务：全局事务回滚/提交、Xid 传递、Dubbo 链路事务、数据一致性
- 🔍 Spring AI 深度功能：聊天对话、流式输出、Tool Calling、ReAct Agent、多模态视觉识别、ChatMemory 多轮对话记忆、PromptTemplate 提示词模板
- 🔍 Spring AI RAG 模块：RAG 检索增强生成全流程
- 🔍 Kafka 4.x 集群消息收发：3 节点 KRaft 集群部署、Producer/Consumer 消息收发

> 使用 `demo-spring-cloud` skill 可执行深度验证脚本（verify-trace.sh / verify-stream.sh / verify-seata.sh），或查看各模块 README 了解详细用法

## 项目概述

基于 **Spring Boot 4.x + Spring Cloud Alibaba 2025.1.x** 的生产级微服务示例项目，包含 16 个模块。

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

Stream 模块依赖 RocketMQ，询问用户是否需要帮助安装和启动。 <br>

**RocketMQ 检查**：
```bash
nc -z 127.0.0.1 9876 && echo "✓ RocketMQ NameServer 已运行" || echo "✗ RocketMQ 未运行"
```

若未安装：
```bash
curl -O https://dist.apache.org/repos/dist/release/rocketmq/5.5.0/rocketmq-all-5.5.0-bin-release.zip
unzip rocketmq-all-5.5.0-bin-release.zip -d $HOME
```
启动 NameServer + Broker 并验证：
```bash
ROCKETMQ_HOME=$(find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' | sort -V | tail -1)
cd "$ROCKETMQ_HOME"
nohup bin/mqnamesrv > namesrv.log 2>&1 &
sleep 5
nohup bin/mqbroker -n localhost:9876 > broker.log 2>&1 &
sleep 10
nc -z 127.0.0.1 9876 && echo "✓ NameServer 已启动" || echo "✗ NameServer 启动失败"
nc -z 127.0.0.1 10911 && echo "✓ Broker 已启动" || echo "✗ Broker 启动失败"
```

### 3. MySQL + Seata Server（仅 Seata 模块需要）

Seata 分布式事务模块依赖 MySQL 和 Seata Server：

**MySQL 检查**：
```bash
mysql -u root -proot1234 -e "SELECT 1"
```

若 MySQL 未安装或密码不对：
```bash
# 安装并启动
brew install mysql
mysql.server start
mysqladmin -u root password 'root1234'
```

> 项目统一使用 root/root1234，若已有 MySQL 且密码不同，请重置密码或自行修改各模块 application.yml 中的数据库配置。

**数据库初始化**：
```bash
mysql -u root -proot1234 -e "CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -proot1234 seata < cloud-seata-sample/all.sql
```

**Seata Server 检查**（端口 8091）：
```bash
nc -z 127.0.0.1 8091 && echo "✓ Seata Server 已运行" || echo "✗ Seata Server 未运行"
```

若 Seata Server 未运行，需从源码构建启动（版本 2.8.0-SNAPSHOT，已修复与项目 Nacos 版本的兼容性问题并预置配置）：
```bash
SEATA_SRC="$HOME/github/seata"
[ ! -d "$SEATA_SRC" ] && mkdir -p "$HOME/github" && git clone https://github.com/javahongxi/seata.git "$SEATA_SRC"
cd "$SEATA_SRC" && ./mvnw clean install -DskipTests -q
nohup ./mvnw -pl server spring-boot:run > /tmp/seata-server.log 2>&1 &
for i in $(seq 1 30); do
  nc -z 127.0.0.1 8091 2>/dev/null && echo "✓ Seata Server 已启动" && break
  sleep 1
done
```

### 4. 安装依赖模块

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

| 模块 | 端口 | 说明 |
|------|------|------|
| cloud-nacos-discovery-sample | 8760 | 服务发现 |
| cloud-nacos-config-sample | 8761 | Nacos Config |
| cloud-gateway-sample | 8764 | 网关 |
| cloud-provider-sample | 8765 | Web Provider |
| cloud-provider-reactive-sample | 8762 | Reactive Provider |
| cloud-provider-dubbo-sample | 50051 | Dubbo Provider |
| cloud-grpc-server-sample | 8090(Web)/9090(gRPC) | gRPC Server |
| cloud-consumer-sample | 8766 | Web Consumer |
| cloud-consumer-reactive-sample | 8763 | Reactive Consumer |

**独立模块（无启动顺序依赖）：**

| 模块 | 端口 | 说明 |
|------|------|------|
| cloud-ai-sample | 8888 | Spring AI，需配置 OPENAI_API_KEY |
| cloud-ai-rag-sample | 8889 | Spring AI · RAG，需 PostgreSQL + pgvector + OPENAI_API_KEY |
| cloud-stream-sample | 8767 | 需先安装并启动 RocketMQ |
| cloud-seata-sample | 18081-18084 + 3 Dubbo | 需 MySQL + Seata Server，含 7 个子模块 |
| cloud-kafka-sample | 8768 | 需 Kafka 4.x 集群（KRaft 模式） |

## 演示与验证

### 1. 普通 Web 服务调用

```bash
# 直接访问 (consumer → provider)
curl 'http://localhost:8766/hi?name=hongxi'
# 通过网关 (gateway → consumer → provider)
curl 'http://localhost:8764/consumer-sample/hi?name=hongxi'
```

### 2. Reactive Web 服务调用

```bash
# 直接访问 (consumer-reactive → provider-reactive)
curl 'http://localhost:8763/hi?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-reactive-sample/hi?name=hongxi'
```

### 3. Dubbo 服务调用

```bash
# consumer → provider-dubbo
curl 'http://localhost:8766/dubbo?name=hongxi'
# consumer-reactive → provider-dubbo
curl 'http://localhost:8763/dubbo?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-sample/dubbo?name=hongxi'
curl 'http://localhost:8764/consumer-reactive-sample/dubbo?name=hongxi'
```

**Dubbo REST 接口：**
```bash
# 直接访问 provider-dubbo
curl http://localhost:50051/api/hello/lily
curl 'http://localhost:50051/api/add?a=1&b=2'
curl -X POST http://localhost:50051/api/echo -H "Content-Type: application/json" -d '{"message":"hi"}'
curl 'http://localhost:50051/api/greet/lily?lang=zh'
# 通过网关
curl http://localhost:8764/provider-dubbo-sample/api/hello/lily
```

### 4. gRPC 服务调用

```bash
# consumer → grpc-server
curl 'http://localhost:8766/grpc?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-sample/grpc?name=hongxi'
```

### 5. Trace 链路追踪

**执行一键验证脚本：**
```bash
bash .qoder/skills/demo-spring-cloud/verify-trace.sh
```
脚本覆盖五条链路，自动完成：检查前置服务 → 逐链路发送带已知 trace ID 的请求 → 对比两端日志中的 trace ID → 自动 trace 生成验证 → 汇总结果

**五条验证链路：**

| 链路 | 入口 | 路径 | 目标服务 | 协议 | trace 传播方式 |
|------|------|------|----------|------|---------------|
| Web → Web | consumer-sample `/hi` | RestTemplate (v1.0) / FeignClient (v2.0) | provider-sample (8765) | HTTP | 自动（两种客户端均支持） |
| Web → gRPC | consumer-sample `/grpc` | gRPC Stub | grpc-server-sample (9090) | gRPC | 自动 |
| Web → Dubbo | consumer-sample `/dubbo` | Dubbo Reference | provider-dubbo-sample (50051) | Dubbo | 自动 |
| Reactive → Reactive | consumer-reactive-sample `/hi` | WebClient | provider-reactive-sample (8762) | HTTP | 手动（controller 读取 traceparent header 后通过 `.headers()` 传递给 WebClient） |
| Reactive → Dubbo | consumer-reactive-sample `/dubbo` | Dubbo Reference | provider-dubbo-sample (50051) | Dubbo | 自动 |

> 验证原理：向各 consumer 端点发送带已知 trace ID 的请求，检查目标服务日志中是否出现相同的 trace ID，确认 Spring Boot Observation + 各框架（RestTemplate / gRPC Interceptor / Dubbo ObservationFilter / WebClient 手动传递）自动或手动传播 trace context。
>
> 前置服务：consumer-sample (8766)、consumer-reactive-sample (8763)、provider-sample (8765)、provider-reactive-sample (8762)、grpc-server-sample (8090)、provider-dubbo-sample（Nacos 注册）

### 6. Nacos Config 动态配置

**前提**：`cloud-nacos-config-sample`（8761）已启动。

按以下步骤逐一验证 Nacos Config 的三大核心能力：基础配置管理、@NacosConfig 注解注入、@ConfigurationProperties + @Value 动态刷新。

#### 6a. Nacos 原生 API

通过 `cloud-nacos-config-sample` 模块（端口 8761）提供的接口管理配置，避免直接调用 Nacos API 的鉴权问题。

```bash
# 发布配置
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
# 获取配置
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
# 监听配置变更
curl 'http://localhost:8761/nacos/listener?dataId=my.city'
# 删除配置
curl 'http://localhost:8761/nacos/removeConfig?dataId=my.city'
```

#### 6b. 演示 @NacosConfig 注解

先用原生 API 发布配置，再访问接口验证：

```bash
# 发布配置：dataId=github.username, content=javahongxi
curl 'http://localhost:8761/nacos/publishConfig?dataId=github.username&content=javahongxi'
# 访问（@NacosConfig 注入字段值）
curl http://localhost:8761/config/hello
# 修改配置后再访问，观察动态刷新
# 删除配置后观察日志（@NacosConfigListener 回调）
```

#### 6c. 演示 @ConfigurationProperties 和 @Value

先用原生 API 发布 Properties 格式配置，再访问接口验证：

```bash
# 发布配置（Properties 格式）
CONTENT="cloud.agent.name=Trae CN
cloud.agent.version=3.3.60
cloud.agent.credits=2000000
cloud.agent.enabled=true
cloud.agent.provider.name=Alibaba
cloud.agent.provider.model=Qwen3.7 Plus
cloud.agent.provider.api-key=xxx123aa"
curl -s -X POST http://localhost:8761/nacos/publishConfig \
  -d "dataId=cloud-agent.properties" \
  -d "type=properties" \
  --data-urlencode "content=$CONTENT"
# 查看 @ConfigurationProperties Bean 绑定结果
curl http://localhost:8761/config/agent
# 查看 @Value + @RefreshScope 注入结果
curl http://localhost:8761/config/value
# 修改配置后再访问，观察动态刷新
```

### 7. Sentinel 网关限流

**前提**：`cloud-gateway-sample`（8764）、`cloud-consumer-sample`（8766）、`cloud-provider-sample`（8765）、`cloud-nacos-config-sample`（8761）已启动。

按以下步骤验证 Sentinel 网关限流：

**Step 1：发布 API 分组配置到 Nacos（group=SENTINEL_GROUP）**
```bash
API_GROUP_JSON='[
  {"apiName":"consumer_reactive_api","predicateItems":[{"pattern":"/consumer-reactive-sample/**","matchStrategy":1}]},
  {"apiName":"consumer_api","predicateItems":[{"pattern":"/consumer-sample/**","matchStrategy":1}]}
]'
curl -s -X POST http://localhost:8761/nacos/publishConfig \
  --data-urlencode 'dataId=cloud.sample.gateway.gw-api-group' \
  --data-urlencode 'group=SENTINEL_GROUP' \
  --data-urlencode 'type=json' \
  --data-urlencode "content=$API_GROUP_JSON"
```

**Step 2：发布限流规则到 Nacos**
```bash
FLOW_JSON='[
  {"resource":"consumer_reactive_api","resourceMode":1,"count":10},
  {"resource":"consumer_api","resourceMode":1,"count":5},
  {"resource":"consumer-reactive-sample","resourceMode":0,"count":20}
]'
curl -s -X POST http://localhost:8761/nacos/publishConfig \
  --data-urlencode 'dataId=cloud.sample.gateway.gw-flow' \
  --data-urlencode 'group=SENTINEL_GROUP' \
  --data-urlencode 'type=json' \
  --data-urlencode "content=$FLOW_JSON"
```

**Step 3：等待配置同步和 Gateway 加载规则**
```bash
sleep 5
```

**Step 4：触发限流验证**（consumer_api 阈值: 5 QPS，快速发 10 次请求）
```bash
for i in $(seq 1 10); do
  RESP=$(curl -s "http://localhost:8764/consumer-sample/hi?name=hongxi")
  echo "请求 $i: $RESP"
done
```
预期结果：前 5 次正常返回，后续请求被 Sentinel 拦截（返回 444 或错误信息），证明网关限流生效。

**Step 5：清理 Sentinel 配置**
```bash
curl -s "http://localhost:8761/nacos/removeConfig?dataId=cloud.sample.gateway.gw-api-group&group=SENTINEL_GROUP"
curl -s "http://localhost:8761/nacos/removeConfig?dataId=cloud.sample.gateway.gw-flow&group=SENTINEL_GROUP"
```

### 8. Sentinel 应用级熔断降级

**前提**：`cloud-consumer-sample`（8766）、`cloud-provider-sample`（8765）、`cloud-nacos-config-sample`（8761）已启动。

按以下步骤验证限流与熔断降级，详细步骤和命令参考项目 README 的 [Sentinel 应用级熔断降级](../../../README.md#-sentinel-应用级熔断降级) 章节。

**三类验证场景：**

| 场景 | 资源名 | 规则类型 | 调用路径 | 预期结果 |
|------|--------|----------|----------|----------|
| 接口限流 | `/hi`（URI 自动注册） | flow | consumer 自身接口 | `Blocked by Sentinel (flow limiting)` |
| Feign 熔断 | `GET:http://provider-sample/hello` | degrade | `version=2.0` | `fallback: service unavailable, name=test` |
| RestTemplate 熔断 | `GET:http://provider-sample`（urlCleaner 去端口后） | degrade | `version=1.0` | `Blocked by Sentinel` |

> `SentinelProtectInterceptor` 为 RestTemplate 创建两个资源（`hostResource` 和 `hostWithPathResource`），`urlCleaner` 去除端口号。降级规则需同时覆盖 `GET:http://provider-sample/hello` 和 `GET:http://provider-sample` 两个资源名。
> 
> **注意**：`grade=2`（异常比例）的 `count` 不能设为 1（判断条件为 `currentRatio > count`，需要 >100% 异常，不可能触发），应设为 0.5。`statIntervalMs` 需设为 10000ms 确保请求落在同一统计窗口内。

### 9. Stream 消息收发（需 RocketMQ）

> 本模块演示 Spring Cloud Stream 六大核心场景：基础消费（Consumer）、定时消息源（Supplier）、消息处理管道（Function）、延迟消息（DELAY header）、顺序消息（orderKey）、事务消息（两阶段提交） |

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
   bash .qoder/skills/demo-spring-cloud/verify-stream.sh
   ```
   脚本会自动完成：清理环境 → 检查 Nacos → 启动 RocketMQ（如未运行）→ 创建 Topic/ConsumerGroup → 打包 → 启动 Stream 模块 → 验证六大场景（基础消费 / 定时消息源 / 消息处理管道 / 延迟消息 / 顺序消息 / 事务消息）

---

**如果用户选择手动操作**，可按以下步骤演示：

```bash
# 启动 Stream 模块后，通过 REST API 交互式验证

# 场景2 验证后停止 Supplier 定时消息源，避免后续场景日志刷屏
curl -s -X POST "http://localhost:8767/actuator/bindings/output2-out-0" -H "Content-Type: application/json" -d '{"state":"STOPPED"}'

# 场景3: 消息处理管道 - 发送消息到 transform 函数（观察大写转换）
curl -X POST "http://localhost:8767/stream/send?message=hello+spring+cloud"
# 预期返回: {"topic":"transformPublish-out-0","message":"hello spring cloud","success":true}
# 日志中观察: 消息转换: hello spring cloud -> [PROCESSED] HELLO SPRING CLOUD

# 场景4: 延迟消息 - 发送延迟消息（delayLevel=2 即 5秒后投递）
curl -X POST "http://localhost:8767/stream/delay?message=hello+delay&delayLevel=2"
# 日志中观察: [延迟消息] 收到: hello delay (时间: ...) — 注意接收时间与发送时间的差值应为约5秒

# 场景5: 顺序消息 - 发送带相同 orderKey 的消息（保证顺序消费）
curl -X POST "http://localhost:8767/stream/fifo?message=order-1&orderKey=order-A"
curl -X POST "http://localhost:8767/stream/fifo?message=order-2&orderKey=order-A"
curl -X POST "http://localhost:8767/stream/fifo?message=order-3&orderKey=order-A"
# 日志中观察: [顺序消息] 收到消息按发送顺序依次被消费

# 场景6: 事务消息 - 发送事务消息（两阶段提交，随机决定提交或回滚）
curl -X POST "http://localhost:8767/stream/tx?message=hello+tx"
# 日志中观察: [事务消息] 执行本地事务 → [事务消息] 本地事务提交 (随机) 或 本地事务回滚 (随机)
# 多次调用可观察到 commit 和 rollback 两种场景
```

### 10. Seata 分布式事务（需 MySQL + Seata Server）

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
   bash .qoder/skills/demo-spring-cloud/verify-seata.sh
   ```
   脚本会自动完成：清理环境 → 检查前置条件 → 初始化数据库 → 打包 → 启动辅助服务 → 发布 Nacos 配置 → 启动 Seata Server → 按依赖顺序启动 7 个微服务（account-dubbo → order-dubbo → business） → 验证分布式事务（回滚 + 提交 + Feign + Dubbo + Xid + 数据一致性）

---

**如果用户选择手动操作**，详细步骤请参考`seata-sample`模块的 README。

### 11. Spring AI 模块

> ⏱️ **耗时提示**：AI 接口调用大模型 API，每次响应通常需 **5~30 秒**，完整演示所有 AI 功能约需 **5~10 分钟**。
> 建议：
> - 所有 AI curl 命令加 `--max-time 60` 防止无限等待
> - 响应内容用 `| head -c 500` 截断，避免刷屏，加速演示
> - 多轮对话演示 **2 轮**即可体现上下文记忆能力，无需执行 3 轮
> - 视觉识别 6 个接口可并行发起（用 `&&` 串联），减少等待

启动前配置 API Key：
```bash
export OPENAI_API_KEY=your-api-key-here
```

启动 AI 模块（端口 8888）：
```bash
# 默认使用 qwen3.7-plus 模型（支持多模态视觉识别）
./mvnw -pl cloud-ai-sample spring-boot:run

# 如需切换其他模型，可通过命令行参数覆盖
./mvnw -pl cloud-ai-sample spring-boot:run -Dspring-boot.run.arguments=--spring.ai.openai.chat.options.model=<模型名>
```

等待 AI 模块就绪（通过 actuator 健康检查）：
```bash
for i in $(seq 1 60); do
  resp=$(curl -s "http://localhost:8888/actuator/health" 2>/dev/null)
  if echo "$resp" | grep -q '"status":"UP"'; then
    echo "AI 模块已就绪 (耗时 ${i}s)"
    break
  fi
  sleep 1
done
```

#### 基础对话与 Agent

中文参数需 URL 编码，使用 `--get --data-urlencode`：
```bash
# 简单聊天
curl --max-time 60 --get --data-urlencode "message=你好" "http://localhost:8888/ai/chat" | head -c 500
# 流式输出
curl --max-time 60 --get --data-urlencode "message=讲一个故事" "http://localhost:8888/ai/chat/stream" | head -c 500
# 结构化输出
curl --max-time 60 --get --data-urlencode "message=张三今年25岁，是软件工程师" "http://localhost:8888/ai/extract"
# 高级用法 - 使用 System Message 设定 AI 角色
curl --max-time 60 --get --data-urlencode "message=Dubbo 3.3 有哪些特性" "http://localhost:8888/ai/advanced/system-message" | head -c 500
# 高级用法 - 提供示例引导 AI
curl --max-time 60 --get --data-urlencode "message=创建一个列表，包含 1, 2, 3" "http://localhost:8888/ai/advanced/few-shot"
# 高级用法 - 多轮对话（需连续发送，AI 会记住上下文）
# 演示 2 轮即可体现上下文记忆，无需执行 3 轮
# 第 1 轮：建立上下文
curl --max-time 60 --get --data-urlencode "message=我喜欢Java和Spring Boot" "http://localhost:8888/ai/advanced/conversation" | head -c 500
# 第 2 轮：基于上文追问（AI 会记住你喜欢 Java）
curl --max-time 60 --get --data-urlencode "message=那我应该用什么技术栈来做微服务" "http://localhost:8888/ai/advanced/conversation" | head -c 500
# 高级用法 - 带温度参数的创意性对话
curl --max-time 60 --get --data-urlencode "message=帮我写一篇春天的故事，不超过300字" "http://localhost:8888/ai/advanced/creative" | head -c 500
# Tool Calling
curl --max-time 60 --get --data-urlencode "message=北京今天天气怎么样？" "http://localhost:8888/ai/tool/weather" | head -c 500
# ReAct Agent
curl --max-time 60 --get --data-urlencode "message=北京天气怎么样？适合出门吗？" "http://localhost:8888/ai/agent/chat" | head -c 500
# MCP Server（SSE 端点）
# 连接地址: http://localhost:8888/sse
```

#### 多模态视觉识别

**🔍 验证前预检查图片 URL 可用性：**

调用视觉接口前，必须先检查以下 6 个图片 URL 是否可访问（curl -I 检查），不可用的需从今日头条、澎湃新闻找替代图片。图片要求：公开可访问、不拒绝 Java `UrlResource` 请求（避免百度图片等限制性 CDN）。
```bash
for url in \
  "https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg" \
  "https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg" \
  "https://p3-search.byteimg.com/obj/pgc-image/94e63ee2f0f840b0813e3746d2a9590b" \
  "https://p3-search.byteimg.com/obj/labis/624fb344cca59ed91d6ada99b45f41ca" \
  "https://p3-search.byteimg.com/obj/labis/9c78113c22823e91536fb63f8f599e13" \
  "https://p3-search.byteimg.com/obj/labis/a7dd04c539c4515b6018e9a39a32be36"; do
  curl -s -o /dev/null -w "%{http_code} $url\n" -L --max-time 10 "$url"
done
```

**🔴 必须逐一演示全部 6 个视觉识别接口，不可跳过：**

| 序号 | 接口 | 说明 |
|------|------|------|
| 1 | `/ai/vision/analyze-url` | URL 图片分析 |
| 2 | `/ai/vision/analyze-upload` | 上传图片分析 |
| 3 | `/ai/vision/ocr` | OCR 文字识别 |
| 4 | `/ai/vision/chart-analysis` | 图表分析 |
| 5 | `/ai/vision/code-from-image` | 代码截图转代码 |
| 6 | `/ai/vision/compare` | 多图片对比 |

```bash
# 1/6 URL 图片分析（澎湃新闻：神舟十号海报）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/analyze-url" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg" | head -c 500

# 2/6 图片上传分析（项目根目录下的架构图）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/analyze-upload" \
  -F "file=@arch.png" | head -c 500

# 3/6 OCR 文字识别（澎湃新闻：北京申奥成功）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/ocr" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg" | head -c 500

# 4/6 图表分析（今日头条：武汉市历年生产总值）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/chart-analysis" \
  -d "imageUrl=https://p3-search.byteimg.com/obj/pgc-image/94e63ee2f0f840b0813e3746d2a9590b" | head -c 500

# 5/6 代码截图转代码（今日头条：Java代码图片）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/code-from-image" \
  -d "imageUrl=https://p3-search.byteimg.com/obj/labis/624fb344cca59ed91d6ada99b45f41ca" | head -c 500

# 6/6 多图片对比分析（今日头条：鞠婧祎 vs 陈都灵）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/compare" \
  -d "imageUrl1=https://p3-search.byteimg.com/obj/labis/9c78113c22823e91536fb63f8f599e13" \
  -d "imageUrl2=https://p3-search.byteimg.com/obj/labis/a7dd04c539c4515b6018e9a39a32be36" | head -c 500
```

> **💡 中文输出**：视觉接口返回的 JSON 中文可能被 Unicode 转义，用以下命令正确显示：
> ```bash
> curl -s -X POST "http://localhost:8888/ai/vision/analyze-url" \
>   -d "imageUrl=..." | python3 -c "import sys, json; print(json.dumps(json.load(sys.stdin), ensure_ascii=False, indent=2))"
> ```

#### DeepSeek 演示接口

> 用于验证同一模块内集成多个大模型提供商（DashScope + DeepSeek）。需额外配置 `export DEEPSEEK_API_KEY=your-key`，未配置时跳过此节。

```shell
# 简单聊天
curl --max-time 60 --get --data-urlencode "message=你好" "http://localhost:8888/deepseek/chat" | head -c 500
# 流式输出
curl --max-time 60 --get --data-urlencode "message=武汉简介" "http://localhost:8888/deepseek/chat/stream" | head -c 500
# 高级用法 - 使用 System Message 设定 AI 角色
curl --max-time 60 --get --data-urlencode "message=Dubbo 3.3 有哪些特性" "http://localhost:8888/deepseek/system-message" | head -c 500
# 高级用法 - 带温度参数的创意性对话
curl --max-time 60 --get --data-urlencode "message=帮我写一篇春天的故事，不超过300字" "http://localhost:8888/deepseek/creative" | head -c 500
# ReAct Agent
curl --max-time 60 --get --data-urlencode "message=北京天气怎么样？适合出门吗？" "http://localhost:8888/deepseek/agent/chat" | head -c 500
```

#### ChatMemory 多轮对话记忆（JDBC 持久化）

基于 `spring-ai-starter-model-chat-memory-repository-jdbc`，对话历史持久化到 PostgreSQL，支持会话隔离。
架构三层：JdbcChatMemoryRepository（持久化）→ MessageWindowChatMemory（滑动窗口 20 条）→ MessageChatMemoryAdvisor（AOP 拦截自动注入/保存历史）。
需前置 PostgreSQL（同 RAG 模块）。

```bash
# 第 1 轮：告诉 AI 你的名字
curl --max-time 60 -X POST http://localhost:8888/ai/memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"session-001","message":"你好，我叫小明"}' | head -c 500

# 第 2 轮：追问，AI 会记住上下文
curl --max-time 60 -X POST http://localhost:8888/ai/memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"session-001","message":"我叫什么名字？"}' | head -c 500
# AI 应回答"小明"，证明记住了上一轮对话

# 不同会话完全隔离（session-002 不知道 session-001 的内容）
curl --max-time 60 -X POST http://localhost:8888/ai/memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"session-002","message":"我叫什么名字？"}' | head -c 500
# AI 不应知道"小明"，证明会话隔离生效

# 清除会话记忆
curl --max-time 60 -X DELETE http://localhost:8888/ai/memory/session-001
```

#### PromptTemplate 提示词模板

使用 Spring AI 的 `PromptTemplate` 进行 `{variable}` 占位符替换，演示三种模板场景。

```bash
# 产品描述生成
curl --max-time 60 -X POST http://localhost:8888/ai/prompt/product \
  -H "Content-Type: application/json" \
  -d '{"product":"Spring AI 实战手册","category":"技术书籍","tone":"专业且幽默"}' | head -c 500

# 代码解释
curl --max-time 60 -X POST http://localhost:8888/ai/prompt/code \
  -H "Content-Type: application/json" \
  -d '{"code":"public record Point(int x, int y) {}","language":"Java","level":"初学者"}' | head -c 500

# 自定义模板（通用入口，支持任意变量）
curl --max-time 60 -X POST http://localhost:8888/ai/prompt/custom \
  -H "Content-Type: application/json" \
  -d '{"template":"请用{language}写一个{function}的示例代码","variables":{"language":"Python","function":"快速排序"}}' | head -c 500
```

### 12. Spring AI RAG 模块（需 PostgreSQL + pgvector）

> ⏱️ **耗时提示**：AI 接口调用大模型 API，每次响应通常需 **5~30 秒**，完整演示约需 **5~10 分钟**。
> 建议：所有 AI curl 命令加 `--max-time 60` 防止无限等待。

**前置条件**：PostgreSQL + pgvector + OPENAI_API_KEY

```bash
# 检查 PostgreSQL 是否运行
pg_isready -h localhost -p 5432 && echo "✓ PostgreSQL 已运行" || echo "✗ PostgreSQL 未运行"

# 若未安装
brew install postgresql
brew install pgvector
brew services start postgresql

# 初始化数据库（创建用户 ai_user、数据库 ai_demo、启用 pgvector 扩展、建表）
psql -U postgres -f cloud-ai-rag-sample/init_ai_demo.sql
```

启动 RAG 模块（端口 8889）：
```bash
export OPENAI_API_KEY=your-api-key-here
./mvnw -pl cloud-ai-rag-sample spring-boot:run
```

等待就绪：
```bash
for i in $(seq 1 60); do
  resp=$(curl -s "http://localhost:8889/actuator/health" 2>/dev/null)
  if echo "$resp" | grep -q '"status":"UP"'; then
    echo "RAG 模块已就绪 (耗时 ${i}s)"
    break
  fi
  sleep 1
done
```

**RAG 检索增强生成全流程**

演示完整的 RAG 流程：文档摄入 → TokenTextSplitter 自动分块 → PgVector 向量化存储 → 相似性检索 → 上下文增强 Prompt → LLM 生成。

```bash
# 1. 摄入第一篇文档
curl --max-time 60 -X POST http://localhost:8889/ai/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"content":"Spring AI is a comprehensive framework for Java developers to build AI-native applications. It provides unified abstractions for Chat (ChatClient), Embedding (EmbeddingModel), Prompt templates (PromptTemplate), Vector storage (VectorStore), and RAG (RetrievalAugmentor). Spring AI supports multiple LLM providers including OpenAI, Anthropic, Azure OpenAI, Ollama. Key features include Function Calling, Structured Output, observability with Micrometer and OpenTelemetry.","source":"spring-ai-docs"}'
# 预期返回: {"source":"spring-ai-docs","chunks":1,"message":"文档摄入成功"}

# 2. 摄入第二篇文档
curl --max-time 60 -X POST http://localhost:8889/ai/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"content":"PgVector is a PostgreSQL extension for vector similarity search. It supports IVFFlat and HNSW index types. IVFFlat divides vectors into lists and searches a subset, good for balance between speed and accuracy. HNSW creates a hierarchical graph for fast approximate nearest neighbor search. PgVector supports cosine distance, inner product, and Euclidean distance metrics. Recommended dimensions: 1536 for OpenAI embeddings.","source":"pgvector-docs"}'
# 预期返回: {"source":"pgvector-docs","chunks":1,"message":"文档摄入成功"}

# 3. RAG 基础查询（topK=3，检索最相关的 3 个文档片段）
curl --max-time 60 --get --data-urlencode "question=What are the core features of Spring AI?" "http://localhost:8889/ai/rag/query?topK=3" | head -c 800
# AI 回答中应包含 Spring AI 的核心特性（来自参考资料）

# 4. topK 对比（topK=1，仅检索 1 个最相关文档）
curl --max-time 60 --get --data-urlencode "question=What are the core features of Spring AI?" "http://localhost:8889/ai/rag/query?topK=1" | head -c 800

# 5. 跨文档语义检索（查询同时涉及两篇文档的内容）
curl --max-time 60 --get --data-urlencode "question=What index types and distance metrics does the vector store support?" "http://localhost:8889/ai/rag/query?topK=2" | head -c 800
# AI 应精确回答 IVFFlat、HNSW 索引类型和 cosine/inner product/Euclidean 距离度量

# 6. 删除文档后 RAG 降级验证（删除所有文档后查询，AI 走纯 LLM 路径）
curl --max-time 60 -X DELETE "http://localhost:8889/ai/rag/documents?source=spring-ai-docs"
curl --max-time 60 -X DELETE "http://localhost:8889/ai/rag/documents?source=pgvector-docs"
curl --max-time 60 --get --data-urlencode "question=What is PgVector?" "http://localhost:8889/ai/rag/query?topK=3" | head -c 800
# 回答中不应出现"参考资料"字样，确认走了纯 LLM 路径
```

> **长文档自动分块验证**：摄入超过 800 token 的长文档，TokenTextSplitter 会自动拆分为多个 chunk。
> ```bash
> # 生成约 12000 字符的长文档（~500 字符重复 200 次）
> LONG_CONTENT=$(python3 -c "print('Spring AI is a comprehensive framework for Java developers. ' * 200)")
> curl --max-time 60 -X POST http://localhost:8889/ai/rag/ingest \
>   -H "Content-Type: application/json" \
>   -d "{\"content\":\"$LONG_CONTENT\",\"source\":\"spring-ai-long-doc\"}"
> # 预期返回 chunks > 1，验证 TokenTextSplitter 自动分块
> ```

### 13. Kafka 4.x 消息收发（需 Kafka 集群）

> 本模块演示 Kafka 4.x 传统 Consumer Group、Share Groups 特性（允许多消费者从同一分区并行消费）和两种确认模式，以及事务消息。
> Kafka 集群需单独部署，详见 [cloud-kafka-sample/README.md](../../../cloud-kafka-sample/README.md)。

**前置条件**：Kafka 4.x 3节点集群已启动（端口 9092/9094/9096）

```bash
# 检查 Kafka 集群是否运行
nc -z 127.0.0.1 9092 2>/dev/null && echo "✓ Kafka Broker 已运行" || echo "✗ Kafka 集群未运行"
```

若 Kafka 集群未启动，请按 cloud-kafka-sample/README.md 中的步骤部署。

**Step 1：创建 Topic（必须，3分区 3副本）：**

```bash
# 在 Kafka 集群中创建所有 topic
KAFKA_HOME=$(find "$HOME" -maxdepth 1 -type d -name 'kafka_*' | sort -V | tail -1)
$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic share-demo-topic --partitions 3 --replication-factor 3
$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic share-demo-topic-explicit --partitions 3 --replication-factor 3
$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic tx-demo-topic --partitions 3 --replication-factor 3
```

**Step 2：启动模块：**

```bash
./mvnw -pl cloud-kafka-sample spring-boot:run
```

**验证消息收发：**

模块启动后，`ApplicationRunner` 自动发送传统 Consumer Group 消息，日志中可观察到：
```
Sent sample message [SampleMessage{id=1, message='test'}] to topic [testTopic]
Received sample message [SampleMessage{id=1, message='test'}]
```

通过 REST 接口触发 Share Group 消息发送：

```bash
# 发送 Share Group 隐式确认消息（默认10条）
curl -X POST "http://localhost:8768/kafka/share/implicit?count=10"

# 发送 Share Group 显式确认消息（默认10条）
curl -X POST "http://localhost:8768/kafka/share/explicit?count=15"
```

查看日志确认 Share Group 消息收发：
```bash
grep -aE "\[Share-" logs/kafka-sample.log | head -50
```
预期输出（日志包含线程名，可验证并发消费）：
```
[Container#1-C-1] [Share-Implicit] Received: SampleMessage{id=1, message='share-task-1'} from partition 0 offset 0
[Container#1-C-2] [Share-Implicit] Received: SampleMessage{id=2, message='share-task-2'} from partition 0 offset 1
[Container#1-C-3] [Share-Implicit] Received: SampleMessage{id=3, message='share-task-3'} from partition 1 offset 0
...
[Container#2-C-4] [Share-Explicit] Received: SampleMessage{id=1, message='explicit-task-1'} from partition 1 offset 0
[Container#2-C-5] [Share-Explicit] Simulating retry for id=5
```

> **验证并发特性**：观察日志中的线程名（如 `Container#2-C-4`、`Container#2-C-5`），
> 同一分区的消息被不同线程并行处理，证明 Share Groups 支持多消费者并发消费同一分区（传统模式仅允许单消费者）。

**验证事务消息：**

事务消息使用独立的 `KafkaTemplate`（配置 `transactional.id`），消费者采用 `read_committed` 隔离级别。

```bash
# 事务提交 - 消费者可读到消息
curl -X POST "http://localhost:8768/kafka/tx/commit?count=5"

# 事务回滚 - 消费者读不到消息
curl -X POST "http://localhost:8768/kafka/tx/rollback?count=5"

# 查看事务消息日志
grep -aE "\[TX" logs/kafka-sample.log | tail -20
```

预期输出：
```
[TX] Sent message [SampleMessage{id=1, message='tx-task-1'}] to topic [tx-demo-topic]
...
[TX] Transaction committed successfully, 5 messages visible to consumers
[TX-Consumer] Received: SampleMessage{id=1, message='tx-task-1'} from partition 0 offset 0
...
[TX] Simulating transaction rollback, messages will NOT be visible to consumers
[TX] Transaction rolled back, messages are NOT visible to consumers: Simulated transaction rollback
```

> **核心特性**：
> - **并行消费**：同一分区的消息可被多个消费者同时处理（传统模式仅允许单消费者）
> - **逐条确认**：支持 ACK/NACK 机制，精确控制每条消息的确认、重试或拒绝
> - **隐式确认**：方法正常返回自动 ACCEPT，抛出异常自动 REJECT
> - **显式确认**：手动调用 `acknowledgment.acknowledge()/release()/reject()` 精细控制
> - **重试演示**：id 为 5 的倍数的消息会触发 release，最多重投递 5 次（Kafka 默认 `group.share.delivery.count.limit=5`）后停止
> - **事务消息**：`executeInTransaction` 原子发送，事务提交前消费者不可见（read_committed 隔离级别）

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| 服务注册失败 | 检查 Nacos 是否启动，环境变量是否设置 |
| 端口冲突 | 检查端口是否被占用，或修改 application.yml 中的端口 |
| gRPC 调用失败 | 确认 grpc-server 的 gRPC 端口 9090 可访问 |
| Sentinel 未限流 | 检查 Nacos 中是否配置了对应的 Sentinel 规则 |
| AI 模块 401 | 检查 OPENAI_API_KEY 是否正确配置 |
| AI 视觉识别 500 | 图片 URL 不可访问（百度图片会拒绝 Java UrlResource 请求），使用稳定可访问的 URL |
| AI 接口 400 | 中文参数需 URL 编码，使用 `--get --data-urlencode` |
| RAG 模块连接 PostgreSQL 失败 | 确认 PostgreSQL 已运行（`pg_isready`），已执行 `init_ai_demo.sql` 初始化数据库 |
| RAG 摄入返回 0 chunks | 检查 content 是否为空，确认 PgVector 扩展已启用（`\connect ai_demo` 后 `CREATE EXTENSION vector`） |
| RAG 查询回答未引用参考资料 | 确认文档已成功摄入（ingest 返回 chunks > 0），检查 topK 参数是否合理 |
| ChatMemory 无记忆效果 | 确认 JDBC 表 `SPRING_AI_CHAT_MEMORY` 已自动创建（ai 模块端口 8888），检查 conversationId 是否一致 |
| Kafka 模块连接失败 | 确认 Kafka 集群已启动（端口 9092/9094/9096），已创建 share-demo-topic、share-demo-topic-explicit 和 tx-demo-topic |
