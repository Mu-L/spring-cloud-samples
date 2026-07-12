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

4. **脚本输出规范**：执行任何项目脚本（`start-all.sh`、`verify-*.sh`）时，**必须完整输出脚本的全部日志**， 禁止使用 `tail -n`、`tail -f`、`head -n` 等方式截断输出。脚本的完整输出是验证结果的依据，截断会导致无法确认每一步是否通过。

5. **演示纪律（强制执行）**：
   - 🔴 **禁止选择性演示**：每个演示场景的所有步骤必须 **逐一执行**，不可跳过任何一步
   - 🔴 **严格按步骤顺序**：必须按 Step 1 → Step 2 → ... 的顺序执行，不可乱序或合并
   - 🔴 **每步必须说明意图并评价结果**：每个 curl 命令执行前，必须用一句话说明 **该请求的目的**（如："接下来验证配置动态刷新：发布新配置后调用接口，观察值是否自动更新"）；执行后必须展示返回结果并进行 **说明或评价**（如："返回了 xxx，符合预期"、"模型正确提取了年龄和职业"、"流式输出逐字显示，体验良好"），不可仅贴出原始响应而不做任何解读
   - 🔴 **禁止用"参考文档"替代执行**：references/ 目录下的文档是操作手册，AI 必须按其中的步骤执行，而非仅列出链接
   - 🔴 **禁止省略 curl 命令**：不可用"同理"、"以此类推"、"省略"等理由跳过任何 curl 命令
   - 🔴 **演示完成后汇总**：所有场景演示完成后，输出汇总表格，列出每个场景的执行状态（✅ 通过 / ❌ 失败）

6. **请求前后说明与评价（强制执行）**：
   - 🔴 每发起一个或一批 curl 请求前，**必须先说明该请求的目的**（验证什么功能、预期返回什么），不可静默发起请求
   - 🔴 请求返回后，**必须立即对结果进行说明或评价**，不可静默跳过
   - 评价内容包括：返回状态是否正常、响应数据是否符合预期、关键信息提取是否准确、与上一步的对比差异等
   - 对于 AI 类接口（chat/stream/extract/agent 等），需评价模型回答的质量（如：回答是否切题、结构化输出是否正确、记忆是否生效）
   - 对于异常结果，需分析可能原因并给出处理建议

7. **原理解读（强制执行）**：
   - 🔴 演示每个功能时，**必须简要说明其背后的技术原理和项目代码实现**，不可只做"执行命令 → 展示结果"的操作工
   - 原理解读应包含：核心组件/注解的作用、数据流转过程、框架自动完成的魔法等
   - 🔴 **代码关联**：每个场景演示时，必须指出项目中使用了什么注解/类/方法来实现该功能（如："这个接口在 AiChatController 中通过 `.entity(PersonInfo.class)` 实现结构化输出"），让用户知其然也知其所以然

---

## 演示执行流程

当用户说"演示本项目"时，按以下流程执行：

1. **环境检查与准备**：仅检查 3 项基本前置条件：JDK → Nacos → 安装依赖模块。其他中间件（MySQL、RocketMQ、Seata Server、Kafka、PostgreSQL、Redis）不在启动前统一检查，而是在对应模块演示时按需准备
2. **服务启动**：执行 `sh start-all.sh` 启动所有核心模块（🔴脚本执行时间较长，必须分阶段读取已完成的输出）
3. **基础验证**：start-all.sh 自动执行服务注册、健康检查、基础调用链路、网关路由验证
4. **深度演示**：按下方"演示与验证"章节的 9 个场景逐一执行（Trace → Nacos Config → Sentinel Gateway → Sentinel App → Stream → Seata → Spring AI → RAG → Kafka）
5. **结果汇总**：输出汇总表格，列出每个场景的执行状态（✅ 通过 / ❌ 失败）

> 用户也可以只演示单个模块，例如："演示 Stream"、"验证 Seata 分布式事务"、"演示 Spring AI"、"演示 Kafka 消息收发"，此时仅执行对应场景的步骤。

### 验证范围

**基础验证**（`sh start-all.sh` 自动执行，无需额外操作）：
- 服务注册发现、健康检查、基础调用链路（Web/Reactive/Dubbo/gRPC）、网关路由、Nacos Config 健康检查、AI/Stream/Seata/Kafka 模块健康检查

**深度演示**（按下方场景逐一执行，每个场景的所有步骤不可跳过）：
- Trace 链路追踪、Nacos Config 动态配置、Sentinel 限流熔断、Stream 消息收发、Seata 分布式事务、Spring AI 全功能、Spring AI RAG、Kafka 4.x 消息收发

## 前置条件

> 🔴 **环境变量清理（必须）**：部分 AI 沙盒环境会注入 `SERVER__PORT` 环境变量，该变量会覆盖所有 Spring Boot 应用（包括 Nacos）的 `server.port` 配置，导致端口错乱。**启动任何服务前，必须先执行：**
> ```bash
> unset SERVER__PORT
> ```

> 🔴 **精简前置原则**：启动前仅检查 **JDK → Nacos → 安装依赖模块** 3 项基本前置条件。其他中间件（MySQL、RocketMQ、Seata Server、Kafka、PostgreSQL、Redis）**不在启动前统一检查**，而是在对应模块演示时按需准备，避免复杂的环境检查阻碍演示流程。

### 1. JDK 17+（必须）

所有模块基于 Spring Boot 4.x，要求 JDK 17 及以上。支持 JDK 17 ~ 21。

**检查 JDK 状态（AI 自动执行）：**
```bash
java -version 2>&1 | grep -oE '"[0-9]+\.' | grep -oE '[0-9]+' | head -1
```

根据输出版本号判断：
- **17 ~ 21** → ✓ JDK 已就绪，跳过安装
- **< 17 或无输出** → 需要安装 JDK 17

**安装 JDK 17（仅在需要时执行）：**
```bash
brew install openjdk@17
sudo ln -sfn $(brew --prefix openjdk@17)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home
```
- **持久化**：检查 `~/.zshrc` 中是否已包含 `JAVA_HOME`，若未包含则追加写入：
```bash
grep -q 'JAVA_HOME' ~/.zshrc 2>/dev/null || cat >> ~/.zshrc << 'EOF'
export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
EOF
```

> 若 Homebrew 未安装，先执行：`/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"`

### 2. Nacos 注册中心（必须）

所有模块依赖 Nacos，启动前先确认 Nacos 已就绪。

**当用户说"安装 Nacos"时，按以下流程执行：**

**Step 1：检查 Nacos 状态**
```bash
curl -s http://127.0.0.1:8848/nacos/actuator/health | grep -q '"status":"UP"' && echo "✓ Nacos 已运行" || echo "✗ Nacos 未运行"
```

根据检查结果处理：

**已运行 ✓** → 跳到 Step 2 切换免密

🔴如果未运行，则必须仔细在本地查找有没有安装，最深的要查4层目录，查找方式如下
```bash
NACOS_START=$(find "$HOME/nacos" "$HOME"/nacos-* "$HOME/ai-infra/nacos" -maxdepth 4 -name 'startup.sh' -path '*/bin/*' 2>/dev/null | head -1)
if [ -n "$NACOS_START" ]; then
  NACOS_HOME=$(dirname "$(dirname "$NACOS_START")")
  cd "$NACOS_HOME" && bin/startup.sh -m standalone
fi
```
如果启动失败，由AI自行排查处理。启动完成后跳到 Step 2。

**未安装** → 下载二进制包并部署（全程非交互，AI 可自主完成）：
```bash
# 1. 下载 Nacos Server zip 包
curl -L -o /tmp/nacos-server-3.2.2.zip 'https://download.nacos.io/nacos-server/nacos-server-3.2.2.zip?file=nacos-server-3.2.2.zip'

# 2. 解压到 $HOME/nacos（覆盖旧版本，方便升级）
unzip -o /tmp/nacos-server-3.2.2.zip -d "$HOME" && rm -f /tmp/nacos-server-3.2.2.zip

# 3. 定位 NACOS_HOME 并配置免密模式 + JWT 密钥 + 服务身份
NACOS_HOME="$HOME/nacos"
CONF="$NACOS_HOME/conf/application.properties"
sed -i '' 's/^nacos.core.auth.enabled=true$/nacos.core.auth.enabled=false/' "$CONF"
sed -i '' 's/^nacos.core.auth.admin.enabled=true$/nacos.core.auth.admin.enabled=false/' "$CONF"
sed -i '' 's/^nacos.core.auth.console.enabled=true$/nacos.core.auth.console.enabled=false/' "$CONF"
# Nacos 3.x 首次启动必须预配置 JWT 密钥和服务身份，否则会交互式提示
sed -i '' 's|^nacos.core.auth.plugin.nacos.token.secret.key=$|nacos.core.auth.plugin.nacos.token.secret.key=VGhpc0lzTXlDdXN0b21TZWNyZXRLZXkwMTIzNDU2Nzg=|' "$CONF"
sed -i '' 's|^nacos.core.auth.server.identity.key=$|nacos.core.auth.server.identity.key=nacosServerIdentityKey2024|' "$CONF"
sed -i '' 's|^nacos.core.auth.server.identity.value=$|nacos.core.auth.server.identity.value=nacosServerIdentityValue2024|' "$CONF"

# 4. 后台启动 Nacos
"$NACOS_HOME/bin/startup.sh" -m standalone
```
> 直接下载 zip 包方式全程非交互，无需用户手动 Ctrl+C。安装完成后已自动配置免密模式，Console 和所有 API 均无需登录鉴权。
> 若下载或启动失败，提示用户手动执行一键安装：`curl -fsSL https://nacos.io/nacos-installer.sh | bash`

**Step 2：切换 Nacos 为免密模式（AI 自动完成）**

> 🔴 **此步骤由 AI 自动执行，无需用户手动操作。**
> Nacos 3.x 支持通过配置关闭鉴权，实现免密访问 Console 和所有 API，无需管理密码。

- **检查鉴权状态**：
```bash
NACOS_DIR=$(find "$HOME/nacos" "$HOME"/nacos-* "$HOME/ai-infra/nacos" -maxdepth 4 -name 'application.properties' -path '*/conf/*' 2>/dev/null | head -1)
if [ -n "$NACOS_DIR" ]; then
  grep -q 'nacos.core.auth.enabled=false' "$NACOS_DIR" && echo "✓ 免密模式已启用" || echo "✗ 鉴权已启用，需切换"
fi
```

- **若已是免密模式** → 跳过，直接到 Step 3 验证
- **若鉴权已启用** → 修改配置关闭鉴权：
```bash
# 定位 Nacos 配置文件
NACOS_DIR=$(find "$HOME/nacos" "$HOME"/nacos-* "$HOME/ai-infra/nacos" -maxdepth 4 -name 'application.properties' -path '*/conf/*' 2>/dev/null | head -1)

# 将三个鉴权开关从 true 改为 false
sed -i '' 's/^nacos.core.auth.enabled=true$/nacos.core.auth.enabled=false/' "$NACOS_DIR"
sed -i '' 's/^nacos.core.auth.admin.enabled=true$/nacos.core.auth.admin.enabled=false/' "$NACOS_DIR"
sed -i '' 's/^nacos.core.auth.console.enabled=true$/nacos.core.auth.console.enabled=false/' "$NACOS_DIR"
```

配置项说明：

| 配置项 | 作用 | 免密值 |
|--------|------|--------|
| `nacos.core.auth.enabled` | SDK/gRPC API 鉴权 | `false` |
| `nacos.core.auth.admin.enabled` | Admin API 鉴权 | `false` |
| `nacos.core.auth.console.enabled` | Console UI 鉴权 | `false` |

- **重启 Nacos 使配置生效**：
```bash
NACOS_HOME=$(dirname "$(dirname "$NACOS_DIR")")
"$NACOS_HOME/bin/shutdown.sh" 2>/dev/null; sleep 2
"$NACOS_HOME/bin/startup.sh" -m standalone
```

> 重启后 Console（http://127.0.0.1:8848/nacos）和所有 API 均无需登录鉴权，应用模块也无需配置 Nacos 用户名密码。

**Step 3：验证**
等待 Nacos 启动完成后再次检查健康状态，确认 `"status":"UP"` 后告知用户 Nacos 已就绪。

### 3. 安装依赖模块

部分模块依赖 `cloud-commons` 和 `cloud-sample-api`，启动前需先安装：
```bash
./mvnw -N install -q && ./mvnw -pl cloud-commons,cloud-sample-api install -DskipTests -q
```

### 4. 按需准备的中间件（演示时检查）

以下中间件**不在启动前统一检查**，而是在对应模块演示时按需检查和准备：

| 中间件 | 依赖模块 | 检查时机 |
|--------|---------|--------|
| MySQL | Seata 模块 | 演示 Seata 分布式事务前检查 |
| RocketMQ | Stream 模块 | 演示 Stream 消息收发前检查 |
| Seata Server | Seata 模块 | 演示 Seata 分布式事务前检查 |
| Kafka 4.x | Kafka 模块 | 演示 Kafka 消息收发前检查 |
| PostgreSQL + pgvector | RAG 模块 | 演示 Spring AI RAG 前检查 |
| Redis | RAG 模块（备选） | 仅在用户切换 Redis 向量库时检查 |

> 🔴 **重型中间件安装规范**：MySQL、RocketMQ、Kafka、PostgreSQL 等重型中间件**禁止 AI 自行下载安装**（下载耗时长、占用带宽）。若检查发现未安装，应提示用户执行：
> ```bash
> sh start-all.sh install  # 自动下载并安装所有中间件 + 打包模块
> ```
> **例外**：Seata Server 由 AI 自动从 GitHub 下载源码并构建，因其为项目专用定制版本。

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

> 脚本流程：检查 Nacos → 安装依赖模块 → 打包 → 按顺序启动所有模块 → 执行验证 → 汇总结果

### 手动逐个启动

启动顺序：**基础设施 → Config → Gateway → Provider → Consumer**

```bash
./mvnw -pl <模块目录> spring-boot:run
```

> 🔴 **后台启动规范（AI 必须遵守）**：
> 后台启动模块时，Bash 工具调用**必须**设置 `is_background=true`，启动后用**独立的** Bash 命令轮询 `/actuator/health` 检查健康状态，不要对启动命令调用 `GetTerminalOutput`。
>
> 示例：
> ```
> Bash(command="cd /path && ./mvnw -pl module spring-boot:run > logs/xx.log 2>&1 & echo $! > .pids/xx.pid", is_background=true)
> # 用独立命令轮询健康检查，不要用 GetTerminalOutput 等待启动命令的输出
> Bash(command="sleep 10 && curl -s http://localhost:PORT/actuator/health")
> ```

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

> 本技能先使用 `start-all.sh` 启动服务并完成**基础验证**（服务注册发现、12 条调用链路、各模块健康检查），再逐一进行以下**深度演示**，
> 覆盖 Trace、Nacos Config、Sentinel、Stream、Seata、AI 等高级功能。每个场景的所有步骤必须逐一执行，不可跳过，每步执行后展示结果并说明是否符合预期。
> 各场景的详细 curl 命令参考 [references/](references/) 目录下对应文档。

### 1. Trace 链路追踪

> 前提：核心 9 模块已启动。

**必做步骤：**

1. 执行 trace 验证脚本（覆盖五条跨服务链路）：
```bash
bash .qoder/skills/demo-spring-cloud/scripts/verify-trace.sh
```
2. 向用户展示脚本完整输出，然后用表格汇总五条链路验证结果：

3. 可选：查看 Prometheus 指标：`curl http://localhost:8766/actuator/prometheus`

> 完整说明参考 [trace.md](references/trace.md)

### 2. Nacos Config 动态配置

> 前提：`cloud-nacos-config-sample`（8761）已启动。

**必做步骤（按顺序）：**

1. **发布配置**：`curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'`
2. **获取配置**：`curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'` → 预期返回 `wuhan`
3. **监听配置变更**：`curl 'http://localhost:8761/nacos/listener?dataId=my.city'`
4. **删除配置**：`curl 'http://localhost:8761/nacos/removeConfig?dataId=my.city'`
5. **@NacosConfig 注解**：先发布 `dataId=github.username, content=javahongxi`，再访问 `curl http://localhost:8761/config/hello` → 预期返回 `Hello, javahongxi`
6. **@ConfigurationProperties**：发布 Properties 格式配置（见 nacos-config.md），访问 `curl http://localhost:8761/config/agent` → 预期返回 JSON
7. **@Value + @RefreshScope**：访问 `curl http://localhost:8761/config/value` → 预期返回绑定值
8. **动态刷新验证**：修改 github.username 配置后再次访问 `/config/hello`，观察值是否更新

> 完整命令参考 [nacos-config.md](references/nacos-config.md)

### 3. Sentinel 网关限流

> 前提：gateway(8764)、consumer(8766)、provider(8765)、nacos-config(8761) 已启动。

**必做步骤（按顺序）：**

1. 发布 API 分组配置到 Nacos（SENTINEL_GROUP）
2. 发布限流规则到 Nacos（consumer_api QPS=5）
3. 等待 5 秒配置同步
4. 快速发 10 次请求验证限流：前 5 次正常，后续被拦截
5. 清理 Sentinel 配置

> 完整 curl 命令参考 [sentinel-gateway.md](references/sentinel-gateway.md)

### 4. Sentinel 应用级熔断降级

> 前提：consumer(8766)、provider(8765)、nacos-config(8761) 已启动。

**必做步骤（按顺序）：**

1. **推送限流规则**（QPS=1）到 Nacos
2. **快速连续调用** `/hi` 接口，第二次被限流 → 预期返回 `Blocked by Sentinel`
3. **推送降级规则**（异常比例 50%，熔断 10s）到 Nacos
4. **停止 provider**：`kill -9 $(cat .pids/provider.pid)`
5. **调用 Feign 路径**触发 fallback → 预期返回 `fallback: service unavailable`
6. **调用 RestTemplate 路径**两次 → 第一次 500，第二次被熔断
7. **清理规则**，恢复 provider

> 完整 curl 命令参考 [sentinel-app.md](references/sentinel-app.md)

### 5. Stream 消息收发

> 🔴 **本场景必须且只需执行一键验证脚本，禁止手动逐步执行。**
> verify-stream.sh 已覆盖全部 6 个场景（含 RocketMQ 检查/启动、Topic 创建、模块打包启动、消息收发验证、进程清理），AI 仅需执行脚本并展示输出。

**唯一必做步骤：**

```bash
bash .qoder/skills/demo-spring-cloud/scripts/verify-stream.sh
```

> ⚠️ **禁止事项**：不可跳过脚本改为手动 curl、不可自行检查/启动 RocketMQ、不可手动创建 Topic、不可手动启动 Stream 模块。脚本内部已处理所有前置准备。

执行完成后，向用户展示脚本完整输出，用表格汇总六个场景验证结果。

### 6. Seata 分布式事务

**按需准备 MySQL + Seata Server（演示前检查）：**
```bash
mysql -u root -proot1234 -e "SELECT 1" &>/dev/null && echo "✓ MySQL 已运行" || echo "✗ MySQL 未运行"
nc -z 127.0.0.1 8091 && echo "✓ Seata Server 已运行" || echo "✗ Seata Server 未运行"
```
若未就绪，参考 [seata.md](references/seata.md) 中的安装和启动步骤。

**执行流程：**

1. **执行一键验证脚本**：
   ```bash
   bash .qoder/skills/demo-spring-cloud/scripts/verify-seata.sh
   ```
   > 脚本自动完成：检查 MySQL/Seata Server → 初始化数据库 → 配置 Nacos → 启动 Seata Server → 启动 7 个微服务 → 验证分布式事务

   如一键脚本不可用，按 [seata.md](references/seata.md) 中的步骤逐一执行：
   - Step 1: 初始化 MySQL 数据库
   - Step 2: 配置 Nacos（seata.properties）
   - Step 3: 启动 Seata Server
   - Step 4: 按三层依赖启动 7 个微服务
   - Step 5: 验证 RestTemplate 链路：`curl http://localhost:18081/seata/rest`
   - Step 6: 验证 Feign 链路：`curl http://localhost:18081/seata/feign`
   - Step 7: 验证 Dubbo 链路：`curl http://localhost:18081/seata/dubbo`
   - Step 8: 检查 Xid 传递一致性（查看日志）
   - Step 9: 验证数据一致性（SQL 查询 account_tbl、storage_tbl、order_tbl）

### 7. Spring AI 模块

> ⏱️ AI 接口调用大模型 API，每次响应通常需 **5~30 秒**。建议所有 curl 命令加 `--max-time 60`。
> 🔴 **以下所有子场景必须逐一演示，不可跳过任何一项。**

> 前提：OPENAI_API_KEY 已配置，cloud-ai-sample（8888）已启动。

**必做步骤（按顺序）：**

1. **基础聊天**：`curl --max-time 60 --get --data-urlencode "message=你好" "http://localhost:8888/ai/chat"`
2. **流式输出**：`curl --max-time 60 --get --data-urlencode "message=讲一个故事" "http://localhost:8888/ai/chat/stream"`
3. **结构化输出**：`curl --max-time 60 --get --data-urlencode "message=张三今年25岁，是软件工程师" "http://localhost:8888/ai/extract"`
4. **System Message**：`curl --max-time 60 --get --data-urlencode "message=Dubbo 3.3 有哪些特性" "http://localhost:8888/ai/advanced/system-message"`
5. **多轮对话**（2 轮即可）：发送 2 次 `/ai/advanced/conversation` 请求，第 2 轮追问验证上下文记忆
6. **Tool Calling**：`curl --max-time 60 --get --data-urlencode "message=北京今天天气怎么样？" "http://localhost:8888/ai/tool/weather"`
7. **ReAct Agent**：`curl --max-time 60 --get --data-urlencode "message=北京天气怎么样？适合出门吗？" "http://localhost:8888/ai/agent/chat"`
8. **视觉识别**（6 个接口全部演示，不可跳过）：
   - 先预检查 6 个图片 URL 可用性
   - URL 图片分析、图片上传分析、OCR 文字识别、图表分析、代码截图转代码、多图片对比
9. **ChatMemory 多轮对话记忆**（需 PostgreSQL）：
   - 第 1 轮告诉 AI 名字，第 2 轮追问验证记忆，第 3 轮验证会话隔离
10. **PromptTemplate 提示词模板**（3 个接口）：
    - 产品描述生成、代码解释、自定义模板
11. **DeepSeek 多提供商**（如已配置 DEEPSEEK_API_KEY）：至少演示 chat + agent

> 完整 curl 命令参考 [spring-ai.md](references/spring-ai.md)

### 8. Spring AI RAG 模块

> ⏱️ AI 接口调用大模型 API，每次响应通常需 **5~30 秒**。建议所有 curl 命令加 `--max-time 60`。
> 🔴 **RAG 全流程必须逐步演示，不可跳过。**

**按需准备 PostgreSQL + pgvector（演示前检查）：**
```bash
psql -U postgres -c "SELECT 1" &>/dev/null && echo "✓ PostgreSQL 已运行" || echo "✗ PostgreSQL 未运行"
```
若未安装：`brew install postgresql@17 pgvector && brew services start postgresql@17 && createuser -s postgres 2>/dev/null || true && psql -U postgres -f cloud-ai-rag-sample/init_ai_demo.sql`

> 前提：PostgreSQL + pgvector 已安装，OPENAI_API_KEY 已配置，cloud-ai-rag-sample（8889）已启动。

**必做步骤（按顺序）：**

1. **摄入第一篇文档**（Spring AI 介绍）→ 确认返回 chunks > 0
2. **摄入第二篇文档**（PgVector 介绍）→ 确认返回 chunks > 0
3. **RAG 基础查询**（topK=3）→ AI 回答中应包含参考资料内容
4. **topK 对比**（topK=1）→ 对比不同检索数量下的回答差异
5. **不同主题文档检索验证**（topK=2）→ 换一篇文档相关问题，AI 应精确回答索引类型和距离度量（验证不是固定返回同一篇）
6. **删除文档后降级验证** → 回答中不应出现"参考资料"字样

> 完整 curl 命令参考 [spring-ai-rag.md](references/spring-ai-rag.md)

### 9. Kafka 4.x 消息收发

**按需准备 Kafka 集群（演示前检查）：**
```bash
nc -z 127.0.0.1 9092 && echo "✓ Kafka 已运行" || echo "✗ Kafka 未运行"
```
若未运行：`bash .qoder/skills/demo-spring-cloud/scripts/kafka.sh start`

> 前提：Kafka 4.x 集群已部署，Topic 已创建，cloud-kafka-sample（8768）已启动。

**必做步骤（按顺序）：**

1. **观察启动日志**：ApplicationRunner 自动发送消息，确认收发成功
2. **Share Groups 隐式确认**：`curl -X POST "http://localhost:8768/kafka/share/implicit?count=10"`
3. **Share Groups 显式确认**：`curl -X POST "http://localhost:8768/kafka/share/explicit?count=15"` → 观察 id=5,10,15 的重投递
4. **查看 Share Group 日志**：`grep -aE "\[Share-" logs/kafka-sample.log | head -50`
5. **事务消息提交**：`curl -X POST "http://localhost:8768/kafka/tx/commit?count=5"` → 消费者可读到
6. **事务消息回滚**：`curl -X POST "http://localhost:8768/kafka/tx/rollback?count=5"` → 消费者读不到
7. **查看事务消息日志**：`grep -aE "\[TX" logs/kafka-sample.log | tail -20`

> Kafka 集群部署和完整命令参考 [kafka.md](references/kafka.md)

## 常见问题

| 问题                     | 解决方案                                                                                             |
|------------------------|--------------------------------------------------------------------------------------------------|
| 服务注册失败                 | 检查 Nacos 是否启动，是否已切换为免密模式（`nacos.core.auth.enabled=false`）                                                      |
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
| Nacos Console 需要登录       | Nacos 应已切换为免密模式，检查 `application.properties` 中 `nacos.core.auth.console.enabled` 是否为 `false`，修改后重启 Nacos            |
