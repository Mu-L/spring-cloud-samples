---
name: demo-spring-cloud
description: >
  启动和演示 Spring Cloud Alibaba 示例项目的各微服务模块。当用户要求演示项目、启动服务、
  验证微服务调用、测试网关路由、查看服务注册、执行集成测试、一键部署、环境检查、
  排查微服务问题、了解 Spring Cloud 组件用法、学习 Nacos/Sentinel/Seata/Dubbo/gRPC/Stream 时
  使用此技能。涵盖 16 个模块的完整演示流程。
tags: [spring-cloud, spring-cloud-alibaba, nacos, sentinel, seata, dubbo, grpc, rocketmq, stream, microservices, demo]
---

# Spring Cloud Alibaba 示例项目演示

## ⚠️ 重要说明

**所有验证操作必须严格按照本 SKILL 的要求执行，特别是：**

1. **Nacos 配置管理**：读写 Nacos 配置时，**必须使用 `cloud-nacos-config-sample` 模块提供的接口**（端口 8761），不要直接使用 Nacos 官方 HTTP API。
   - ✅ 正确方式：`http://localhost:8761/nacos/publishConfig`、`http://localhost:8761/nacos/getConfig`
   - ❌ 错误方式：`http://localhost:8080/nacos/v1/cs/configs`

2. **验证流程**：按照 SKILL 中定义的步骤顺序执行，不要跳过任何前置检查或验证环节。

3. **端口规范**：项目已统一端口分配，AI 模块使用 8888 端口，禁止使用 8080 端口。

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
- 🔍 Sentinel 限流：规则配置、限流效果验证
- 🔍 Stream 消息消费：实际消费逻辑、多 Topic 处理、Consumer Group 行为
- 🔍 Seata 分布式事务：全局事务回滚/提交、Xid 传递、数据一致性
- 🔍 Spring AI 深度功能：聊天对话、流式输出、Tool Calling、ReAct Agent、多模态视觉识别

> 使用 `demo-spring-cloud` skill 可执行深度验证脚本（verify-trace.sh / verify-nacos-config.sh / verify-sentinel-gateway.sh / verify-stream.sh / verify-seata.sh），或查看各模块 README 了解详细用法

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

根据检查结果，进入对应场景：

---

#### 场景 A：Nacos 已运行 ✓

直接跳到 [Step 4：设置环境变量](#step-4设置环境变量)。

---

#### 场景 B：Nacos 已安装但未运行

查找已安装的 Nacos 目录：

```bash
NACOS_DIR=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
echo "找到 Nacos: $NACOS_DIR"
```

启动 Nacos：

```bash
cd "$NACOS_DIR"
bin/startup.sh -m standalone
```

等待启动完成后，跳到 [Step 4：设置环境变量](#step-4设置环境变量)。

---

#### 场景 C：Nacos 未安装

一键安装（自动处理下载、JDK 检查、Derby 初始化、鉴权配置和环境验证）：
```bash
curl -fsSL https://nacos.io/nacos-installer.sh | bash
```

安装完成后使用如下命令部署单机版
```shell
# 本地一键部署一个最新版本的单机 Nacos
nacos-setup
```

> 部署完成后会自动创建 Nacos 账号（用户名：nacos），密码是无规律字符串。
> 部署完成后跳到 [Step 4：设置环境变量](#step-4设置环境变量)。

首次部署后，会自动打开浏览器访问 http://127.0.0.1:8080/ ，使用刚才创建的账号登录

#### Nacos Console（Web 控制台）

登录 http://127.0.0.1:8080/ 后可通过浏览器访问 Nacos Console：

| 功能 | 路径 | 说明 |
|------|------|------|
| 配置管理 | 配置管理 → 配置列表 | 发布、编辑、删除、搜索配置，查看历史版本 |
| 服务管理 | 服务管理 → 服务列表 | 查看已注册服务、实例列表、健康状态 |
| 命名空间 | 命名空间 | 创建/管理命名空间，实现环境隔离 |

> 本项目的 Sentinel 限流规则、Seata 配置等均可通过 Console 管理，比 CLI 更直观。

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

启动步骤：
```bash
ROCKETMQ_HOME=$(find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' | sort -V | tail -1)
cd "$ROCKETMQ_HOME"

# 启动 NameServer
nohup bin/mqnamesrv > namesrv.log 2>&1 &
sleep 5

# 启动 Broker
nohup bin/mqbroker -n localhost:9876 > broker.log 2>&1 &
sleep 10

# 验证
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

若 Seata Server 未运行，需从源码构建启动：

**Seata Server 源码启动方式**：

```bash
# 版本：2.8.0-SNAPSHOT（已预置 Nacos 配置，无需修改 application.yml）

# 1. 查找或克隆 Seata 源码
SEATA_SRC="$HOME/github/seata"
if [ ! -d "$SEATA_SRC" ]; then
  echo "Seata 源码不存在，正在克隆..."
  mkdir -p "$HOME/github"
  git clone https://github.com/javahongxi/seata.git "$SEATA_SRC"
fi

# 2. 构建（首次或代码更新时需要）
cd "$SEATA_SRC"
./mvnw clean install -DskipTests -q

# 3. 启动 Seata Server（非 fat jar，需用 mvnw spring-boot:run）
cd "$SEATA_SRC"
nohup ./mvnw -pl server spring-boot:run > /tmp/seata-server.log 2>&1 &
echo "Seata Server 启动中..."

# 4. 等待启动完成（检查端口 8091）
for i in $(seq 1 30); do
  if nc -z 127.0.0.1 8091 2>/dev/null; then
    echo "✓ Seata Server 已启动 (端口 8091)"
    break
  fi
  sleep 1
done
```

**为什么需要从源码启动**：
- 项目使用的 Nacos 版本与 Seata 官方发布的二进制包存在兼容性问题
- 该克隆版本已修复兼容性问题并预置了 Nacos 配置

### 4. 安装依赖模块

部分模块依赖 `cloud-commons` 和 `cloud-sample-api`，启动前需先安装：
```bash
./mvnw -N install -q && ./mvnw -pl cloud-commons,cloud-sample-api install -DskipTests -q
```

## 启动方式

> **优先级：AI Skill > 脚本 > 手动**

| 方式 | 说明 | 适用场景 |
|------|------|----------|
| 🤖 **AI Skill（推荐）** | 告诉 AI "演示项目"，自动完成环境检查、启动、验证全流程 | 快速体验、集成测试 |
| 📜 **一键脚本** | 通过 `start-all.sh` 自动化启动和验证 | 批量验证、CI/CD |
| 🔧 **手动启动** | 逐个模块手动启动，灵活控制 | 学习调试、单模块开发 |

### 方式一：AI Skill（推荐）

直接告诉 AI 助手你要做什么，例如：
- "演示项目"
- "启动所有服务并验证"
- "验证 Seata 分布式事务"
- "验证 Stream 消息收发"

AI 会自动检查环境、安装依赖、启动服务、执行验证。无需手动操作。

### 方式二：一键脚本

前置条件参考上方「前置条件」章节，脚本会自动检查并尝试启动缺失的组件。

```bash
sh start-all.sh install  # 检查并安装中间件 + 打包模块
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

### 方式三：手动逐个启动

前置条件参考上方「前置条件」章节。

启动顺序原则：**基础设施 → Provider → Consumer → Client → Config**

| 顺序 | 模块 | 端口 | 说明 |
|------|------|------|------|
| 1 | cloud-nacos-discovery-sample | 8760 | 服务发现 |
| 2 | cloud-gateway-sample | 8764 | 网关 |
| 3 | cloud-provider-sample | 8765 | Web Provider |
| 4 | cloud-provider-reactive-sample | 8762 | Reactive Provider |
| 5 | cloud-provider-dubbo-sample | 50051 | Dubbo Provider |
| 6 | cloud-grpc-server-sample | 8090(Web)/9090(gRPC) | gRPC Server |
| 7 | cloud-consumer-sample | 8766 | Web Consumer |
| 8 | cloud-consumer-reactive-sample | 8763 | Reactive Consumer |
| 9 | cloud-consumer-dubbo-sample | - | Dubbo Consumer |
| 10 | cloud-grpc-client-sample | - | gRPC Client |
| 11 | cloud-nacos-config-sample | 8761 | Nacos Config |

单独启动某模块：
```bash
./mvnw -pl <模块目录> spring-boot:run
```

### 独立模块（无启动顺序依赖）

| 模块 | 端口          | 说明 |
|------|-------------|------|
| cloud-ai-sample | 8888        | Spring AI，需配置 OPENAI_API_KEY |
| cloud-stream-sample | 8767        | 需先安装并启动 RocketMQ |
| cloud-seata-sample | 18081-18084 | 需 MySQL + Seata Server |

## 演示与验证

### 1. Nacos Discovery 服务发现

```bash
curl http://localhost:8760/discovery/services
```

### 2. 普通 Web 服务调用

```bash
# 直接访问 (consumer → provider)
curl 'http://localhost:8766/hi?name=hongxi'
# 通过网关 (gateway → consumer → provider)
curl 'http://localhost:8764/consumer-sample/hi?name=hongxi'
```

### 3. Reactive Web 服务调用

```bash
# 直接访问 (consumer-reactive → provider-reactive)
curl 'http://localhost:8763/hi?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-reactive-sample/hi?name=hongxi'
```

### 4. Dubbo 服务调用

```bash
# consumer → provider-dubbo
curl 'http://localhost:8766/dubbo?name=hongxi'
# consumer-reactive → provider-dubbo
curl 'http://localhost:8763/dubbo?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-sample/dubbo?name=hongxi'
curl 'http://localhost:8764/consumer-reactive-sample/dubbo?name=hongxi'
```

### 5. gRPC 服务调用

```bash
# consumer → grpc-server
curl 'http://localhost:8766/grpc?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-sample/grpc?name=hongxi'
```

#### gRPC 负载均衡验证（Nacos 服务发现 + round_robin）

> 通过启动两个 gRPC Server 实例，验证 gRPC 客户端基于 Nacos 服务发现的负载均衡。
> `GreeterImpl` 响应中携带端口号，便于观察请求分发效果。

1. **确保第一个实例已启动**（端口 9090，start-all.sh 已启动）
2. **启动第二个实例**（端口 9091）：
   ```bash
   java -jar cloud-grpc-server-sample/target/cloud-grpc-server-sample.jar \
     --spring.grpc.server.port=9091 --server.port=8091 > logs/grpc-server-2.log 2>&1 &
   echo $! > .pids/grpc-server-2.pid
   ```
3. **等待注册完成后，多次调用观察负载均衡**：
   ```bash
   for i in $(seq 1 6); do curl -s 'http://localhost:8766/grpc?name=hongxi'; echo; done
   ```
   预期响应交替出现 `from port 9090` 和 `from port 9091`（round_robin 策略）
4. **验证完毕后关闭第二个实例**：
   ```bash
   kill $(cat .pids/grpc-server-2.pid) 2>/dev/null; rm -f .pids/grpc-server-2.pid
   ```

### 6. Dubbo REST 接口

```bash
# 直接访问 provider-dubbo
curl http://localhost:50051/api/hello/lily
curl 'http://localhost:50051/api/add?a=1&b=2'
curl -X POST http://localhost:50051/api/echo -H "Content-Type: application/json" -d '{"message":"hi"}'
curl 'http://localhost:50051/api/greet/lily?lang=zh'
# 通过网关
curl http://localhost:8764/provider-dubbo-sample/api/hello/lily
```

### 7. 纯 Dubbo / gRPC 演示

启动后观察日志即可：
- `cloud-consumer-dubbo-sample`：日志中出现 `Hello, lily` 表示调用成功
- `cloud-grpc-client-sample`：日志中出现 `Hello, lily` 表示调用成功

### 8. Trace 链路追踪

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

### 9. Nacos Config 动态配置

**执行一键验证脚本：**
```bash
bash .qoder/skills/demo-spring-cloud/verify-nacos-config.sh
```
脚本会自动完成：检查 nacos-config 模块 → 基础配置管理（发布/读取/删除）→ @NacosConfig 注解注入与动态刷新 → @ConfigurationProperties + @Value + @RefreshScope 绑定与动态刷新 → 清理测试配置

> 以下为手动演示步骤，供学习参考。

#### 9a. Nacos 原生 API

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

#### 9b. 演示 @NacosConfig 注解

先用原生 API 发布配置，再访问接口验证：

```bash
# 发布配置：dataId=github.username, content=javahongxi
curl 'http://localhost:8761/nacos/publishConfig?dataId=github.username&content=javahongxi'
# 访问（@NacosConfig 注入字段值）
curl http://localhost:8761/config/hello
# 修改配置后再访问，观察动态刷新
# 删除配置后观察日志（@NacosConfigListener 回调）
```

#### 9c. 演示 @ConfigurationProperties 和 @Value

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

### 10. Sentinel 网关限流

**前提**：`cloud-gateway-sample`（8764）、`cloud-consumer-sample`（8766）、`cloud-provider-sample`（8765）、`cloud-nacos-config-sample`（8761）已启动。

**执行一键验证脚本：**
```bash
bash .qoder/skills/demo-spring-cloud/verify-sentinel-gateway.sh
```
脚本会自动完成：检查前置服务 → 发布 gw-api-group/gw-flow 配置到 SENTINEL_GROUP → 验证配置写入 → 触发限流（10 次请求，consumer_api 阈值 5 QPS）→ 清理 Sentinel 配置

配置 JSON 参考项目 README 的 [Sentinel Gateway 演示](../../../README.md#-sentinel-gateway-演示) 章节。

### 11. Stream 消息收发（需 RocketMQ）

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
   脚本会自动完成：清理环境 → 检查 Nacos → 启动 RocketMQ（如未运行）→ 创建 Topic/ConsumerGroup → 打包 → 启动 Stream 模块 → 验证消息收发（stream-demo-topic + stream-demo-topic2）

---

**如果用户选择手动操作**，详细步骤请参考项目 README 的 [Stream 演示](../../../README.md#-stream-演示) 章节。

### 12. Seata 分布式事务（需 MySQL + Seata Server）

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
   > 5. 启动 4 个微服务并验证分布式事务
   > 
   > 是否需要我帮您自动完成以上操作？"

   **如果用户同意**，直接执行一键验证脚本：
   ```bash
   bash .qoder/skills/demo-spring-cloud/verify-seata.sh
   ```
   脚本会自动完成：清理环境 → 检查前置条件 → 初始化数据库 → 打包 → 启动辅助服务 → 发布 Nacos 配置 → 启动 Seata Server → 并行启动 4 个微服务 → 验证分布式事务（回滚 + 提交 + Feign + Xid + 数据一致性）

---

**如果用户选择手动操作**，详细步骤请参考`seata-sample`模块的 README。

### 13. Spring AI 模块

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

调用视觉接口前，**必须先检查图片 URL 是否可访问**，避免验证失败浪费时间：
```bash
echo "=== 图片 URL 可用性预检查 ==="
for url in \
  "https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg" \
  "https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg" \
  "https://p3-search.byteimg.com/obj/pgc-image/94e63ee2f0f840b0813e3746d2a9590b" \
  "https://p3-search.byteimg.com/obj/labis/624fb344cca59ed91d6ada99b45f41ca" \
  "https://p3-search.byteimg.com/obj/labis/9c78113c22823e91536fb63f8f599e13" \
  "https://p3-search.byteimg.com/obj/labis/a7dd04c539c4515b6018e9a39a32be36"; do
  status=$(curl -s -o /dev/null -w "%{http_code}" -L --max-time 10 "$url" 2>/dev/null)
  if [ "$status" = "200" ]; then
    echo "✓ [$status] $url"
  else
    echo "✗ [$status] $url — 不可用，需寻找替代图片"
  fi
done
```
- 全部返回 200 → 直接开始验证
- 某 URL 不可用 → **必须先找到替代图片**，建议从今日头条、澎湃新闻等网站找图片。
- 图片要求：公开可访问、不拒绝 Java `UrlResource` 请求（避免百度图片等限制性 CDN）

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
