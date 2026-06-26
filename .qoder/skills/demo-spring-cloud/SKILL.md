---
name: demo-spring-cloud
description: 启动和演示 Spring Cloud Alibaba 示例项目的各微服务模块。当用户要求演示项目、启动服务、验证微服务调用、测试网关路由、查看服务注册、或执行集成测试时使用此技能。涵盖 Nacos、Gateway、Dubbo、gRPC、Sentinel、Stream、Seata、Spring AI 等模块的完整演示流程。
---

# Spring Cloud Alibaba 示例项目演示

## 项目概述

基于 **Spring Boot 4.x + Spring Cloud Alibaba 2025.1.x** 的生产级微服务示例项目，包含 16 个模块。

## 前置条件

### 1. Nacos 注册中心（必须）

所有模块依赖 Nacos，启动前先确认 Nacos 已就绪：
```bash
curl -s http://127.0.0.1:8848/nacos/actuator/health | grep -q '"status":"UP"' && echo "✓ Nacos 已运行" || echo "✗ Nacos 未运行"
```

若未安装，执行一键安装：
```bash
curl -fsSL https://nacos.io/nacos-installer.sh | bash
```

安装后部署（首次需要，后续重启无需重复）：
```bash
nacos-setup
```
> `nacos-setup` 会自动部署单机实例并创建密码（用户名：nacos），该密码写入内置数据库。

若已安装但未运行，在用户目录下查找并启动：
```bash
NACOS_DIR=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
cd "$NACOS_DIR"
bin/startup.sh -m standalone
```

停止 Nacos：
```bash
NACOS_DIR=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
cd "$NACOS_DIR"
bin/shutdown.sh
```

设置环境变量（用户名/密码为 nacos-setup 创建的凭证）：
```bash
export SPRING_CLOUD_NACOS_USERNAME=nacos
export SPRING_CLOUD_NACOS_PASSWORD=<nacos-setup 创建的密码>
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
# 安装并启动（macOS）
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

| 模块 | 端口 | 说明 |
|------|------|------|
| cloud-ai-sample | 8080 | Spring AI，需配置 OPENAI_API_KEY |
| cloud-stream-sample | - | 需先安装并启动 RocketMQ |
| cloud-seata-sample | 18081-18084 | 需 MySQL + Seata Server |

## 演示与验证

### 1. Nacos Discovery 服务发现

```bash
curl http://localhost:8760/discovery/instances
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
```

### 5. gRPC 服务调用

```bash
# consumer → grpc-server
curl 'http://localhost:8766/grpc?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-sample/grpc?name=hongxi'
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

### 8. Nacos Config 动态配置

```bash
# 发布配置
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
# 获取配置
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
```

### 9. Sentinel 网关限流

**前提**：`cloud-gateway-sample`（8764）、`cloud-consumer-sample`（8766）、`cloud-provider-sample`（8765）、`cloud-nacos-config-sample`（8761）已启动。

通过 nacos-config 模块的配置管理接口写入 Sentinel 限流配置，配置 JSON 参考项目 README 的 [Sentinel Gateway 演示](../../README.md#-sentinel-gateway-演示) 章节。

**步骤一：发布自定义 API 分组**（group: `SENTINEL_GROUP`, dataId: `cloud.sample.gateway.gw-api-group`, type: `json`）

content 为项目 README [Sentinel Gateway 演示](../../README.md#-sentinel-gateway-演示) 中 `gw-api-group` 的 JSON 内容：
```bash
curl -X POST 'http://localhost:8761/nacos/publishConfig' \
  --data-urlencode 'dataId=cloud.sample.gateway.gw-api-group' \
  --data-urlencode 'group=SENTINEL_GROUP' \
  --data-urlencode 'type=json' \
  --data-urlencode 'content=<README 中 gw-api-group 的 JSON>'
```

**步骤二：发布流控规则**（group: `SENTINEL_GROUP`, dataId: `cloud.sample.gateway.gw-flow`, type: `json`）

content 为项目 README [Sentinel Gateway 演示](../../README.md#-sentinel-gateway-演示) 中 `gw-flow` 的 JSON 内容：
```bash
curl -X POST 'http://localhost:8761/nacos/publishConfig' \
  --data-urlencode 'dataId=cloud.sample.gateway.gw-flow' \
  --data-urlencode 'group=SENTINEL_GROUP' \
  --data-urlencode 'type=json' \
  --data-urlencode 'content=<README 中 gw-flow 的 JSON>'
```

**步骤三：验证配置写入成功**
```bash
curl 'http://localhost:8761/nacos/getConfig?dataId=cloud.sample.gateway.gw-api-group&group=SENTINEL_GROUP'
curl 'http://localhost:8761/nacos/getConfig?dataId=cloud.sample.gateway.gw-flow&group=SENTINEL_GROUP'
```

**步骤四：触发限流验证**

在浏览器快速刷新访问如下接口，触发限流时返回：
```text
http://localhost:8764/consumer-sample/hi?name=hongxi
```
```json
{"code":444,"msg":"Sentinel gateway block"}
```

**步骤五：验证完成后清理 Sentinel 配置**
```bash
curl 'http://localhost:8761/nacos/removeConfig?dataId=cloud.sample.gateway.gw-api-group&group=SENTINEL_GROUP'
curl 'http://localhost:8761/nacos/removeConfig?dataId=cloud.sample.gateway.gw-flow&group=SENTINEL_GROUP'
```

### 10. Spring AI 模块

**前置步骤：停止所有已运行的服务**（避免端口冲突，AI 模块使用 8080 端口）：
```bash
# 在项目根目录下执行
sh start-all.sh stop
```

启动前配置 API Key：
```bash
export OPENAI_API_KEY=your-api-key-here
```

启动 AI 模块（端口 8080）：
```bash
# 默认模型启动
./mvnw -pl cloud-ai-sample spring-boot:run

# 指定模型启动（多模态/视觉识别需使用支持视觉的模型，如 qwen3.7-plus）
./mvnw -pl cloud-ai-sample spring-boot:run -Dspring-boot.run.arguments=--spring.ai.openai.chat.options.model=qwen3.7-plus
```

等待 AI 模块就绪（通过 actuator 健康检查）：
```bash
for i in $(seq 1 60); do
  resp=$(curl -s "http://localhost:8080/actuator/health" 2>/dev/null)
  if echo "$resp" | grep -q '"status":"UP"'; then
    echo "AI 模块已就绪 (耗时 ${i}s)"
    break
  fi
  sleep 1
done
```

常用演示接口（中文参数需 URL 编码，使用 `--get --data-urlencode`）：
```bash
# 简单聊天
curl --get --data-urlencode "message=你好" "http://localhost:8080/ai/chat"
# 流式输出
curl --get --data-urlencode "message=讲一个故事" "http://localhost:8080/ai/chat/stream"
# 结构化输出
curl --get --data-urlencode "text=张三今年25岁，是软件工程师" "http://localhost:8080/ai/extract"
# Tool Calling
curl --get --data-urlencode "question=北京今天天气怎么样？" "http://localhost:8080/ai/tool/weather"
# ReAct Agent
curl --get --data-urlencode "question=北京天气怎么样？适合出门吗？" "http://localhost:8080/ai/agent/chat"
# MCP Server（SSE 端点）
# 连接地址: http://localhost:8080/sse
```

#### 多模态视觉识别（需使用支持视觉的模型）

启动时通过命令行参数指定模型（无需修改 application.yml）：
```bash
java -jar cloud-ai-sample/target/cloud-ai-sample.jar --spring.ai.openai.chat.options.model=qwen3.7-plus
```

支持的视觉模型示例：`qwen3.7-plus`、`qwen-vl-max`、`qwen-vl-plus` 等。

```bash
# 通过 URL 分析图片（神舟十号海报）
curl -X POST "http://localhost:8080/ai/vision/analyze-url" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg"

# 图片上传分析（项目根目录下的架构图）
curl -X POST "http://localhost:8080/ai/vision/analyze-upload" \
  -F "file=@arch.png"

# OCR 文字识别
curl -X POST "http://localhost:8080/ai/vision/ocr" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg"

# 图表分析（QuickChart.io 生成的柱状图）
curl -X POST "http://localhost:8080/ai/vision/chart-analysis" \
  -d "imageUrl=https://quickchart.io/chart?c=%7Btype%3A%27bar%27%2Cdata%3A%7Blabels%3A%5B%27Q1%27%2C%27Q2%27%2C%27Q3%27%2C%27Q4%27%5D%2Cdatasets%3A%5B%7Blabel%3A%27Revenue%27%2Cdata%3A%5B100%2C200%2C150%2C300%5D%7D%5D%7D%7D"

# 代码截图转代码（CSDN C语言代码图片）
curl -X POST "http://localhost:8080/ai/vision/code-from-image" \
  -d "imageUrl=https://i-blog.csdnimg.cn/blog_migrate/486ded85cb954f0da650e7f9c306900e.png"

# 多图片对比分析（鞠婧祎 vs 陈都灵）
curl -X POST "http://localhost:8080/ai/vision/compare" \
  -d "imageUrl1=https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0519%2F741a10acj00swie1b004ed200u00140g00zk01be.jpg&thumbnail=660x2147483647&quality=80&type=jpg" \
  -d "imageUrl2=https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0328%2Ffbc14108j00sttzpb002ld000yg00uem.jpg&thumbnail=660x2147483647&quality=80&type=jpg"
```

> 注意：百度图片等部分 CDN 会拒绝 Java `UrlResource` 的请求（无 User-Agent），导致 500 错误。
> 以上 URL 已验证可稳定访问。如需使用其他图片，请确保 URL 可被服务端直接下载。

**验证完成后停止 AI 模块**：
```bash
# 查找并停止 AI 模块进程
pkill -f "cloud-ai-sample" && echo "✓ AI 模块已停止" || echo "AI 模块未运行"
```

### 11. Stream 消息收发（需 RocketMQ）

**执行流程：**

0. **停止所有已运行的服务**（避免端口冲突和资源占用）：
   ```bash
   sh start-all.sh stop
   ```

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

**如果用户选择手动操作**，详细步骤请参考项目 README 的 [Stream 演示](../../README.md#-stream-演示) 章节。

### 12. Seata 分布式事务（需 MySQL + Seata Server）

**执行流程：**

0. **停止所有已运行的服务**（避免端口冲突和资源占用）：
   ```bash
   # 在项目根目录下执行
   sh start-all.sh stop
   ```

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

**如果用户选择手动操作**，详细步骤请参考项目 README 的 [Seata 演示](../../README.md#-seata-分布式事务演示) 章节。

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

## 分支说明

- `springboot3`：基于 Spring Boot 3.5.0+ 的示例
- `eureka`：使用 Eureka 作为注册中心的初始版本
