---
name: demo-spring-cloud
description: 启动和演示 Spring Cloud Alibaba 示例项目的各微服务模块。当用户要求演示项目、启动服务、验证微服务调用、测试网关路由、查看服务注册、或执行集成测试时使用此技能。涵盖 Nacos、Gateway、Dubbo、gRPC、Sentinel、Stream、Spring AI 等模块的完整演示流程。
---

# Spring Cloud Alibaba 示例项目演示

## 项目概述

基于 **Spring Boot 4.x + Spring Cloud Alibaba 2025.1.x** 的生产级微服务示例项目，包含 16 个模块。

## 前置条件

### 1. Nacos 注册中心（必须）

所有模块依赖 Nacos，启动前先确认 Nacos 已就绪：

```bash
curl -s http://127.0.0.1:8848/nacos/actuator/health
```

若未启动，提示用户先部署 Nacos，并设置环境变量：
```bash
export SPRING_CLOUD_NACOS_USERNAME=your_username
export SPRING_CLOUD_NACOS_PASSWORD=your_password
```

### 2. RocketMQ（仅 Stream 模块需要）

Stream 模块依赖 RocketMQ，询问用户是否需要帮助安装和启动。 <br>
安装与启动指南请参考项目 README 的 [Stream 演示](../../README.md#-stream-演示) 章节。

简要步骤：
1. 下载 RocketMQ 并解压
2. 启动 NameServer 和 Broker
3. 创建 Topic 和 Consumer Group
4. 启动 Stream 模块，观察日志验证消息收发

### 3. MySQL + Seata Server（仅 Seata 模块需要）

Seata 分布式事务模块依赖 MySQL 和 Seata Server：

**MySQL 检查**：
```bash
mysql -u root -proot1234 -e "SELECT 1"
```

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
# Seata Server 不是 Spring Boot 应用，是 mvn exec:java 方式启动
# 版本：2.8.0-SNAPSHOT

# 1. 查找或克隆 Seata 源码
SEATA_SRC="$HOME/github/incubator-seata"
if [ ! -d "$SEATA_SRC" ]; then
  echo "Seata 源码不存在，正在克隆..."
  mkdir -p "$HOME/github"
  git clone https://github.com/apache/incubator-seata.git "$SEATA_SRC"
fi

# 2. 替换 Seata Server 的 application.yml（使用 Nacos 作为配置中心和注册中心）
# 配置文件路径：$SEATA_SRC/server/src/main/resources/application.yml
cat > "$SEATA_SRC/server/src/main/resources/application.yml" << 'EOF'
server:
  port: 8091
spring:
  application:
    name: seata-server
  main:
    web-application-type: none
logging:
  config: classpath:logback-spring.xml
  file:
    path: ${log.home:${user.home}/logs/seata}
  extend:
    logstash-appender:
      # off by default
      enabled: false
      destination: 127.0.0.1:4560
    kafka-appender:
      # off by default
      enabled: false
      bootstrap-servers: 127.0.0.1:9092
      topic: logback_to_logstash
      producer:
        acks: 0
        linger-ms: 1000
        max-block-ms: 0
    metric-appender:
      # off by default
      enabled: false

seata:
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      username: ${spring.cloud.nacos.username}
      password: ${spring.cloud.nacos.password}
      group: SEATA_GROUP
      namespace: public
      data-id: seata.properties
  registry:
    type: nacos
    nacos:
      application: seata-server
      group: SEATA_GROUP
      namespace: public
      cluster: default
      server-addr: 127.0.0.1:8848
      username: ${spring.cloud.nacos.username}
      password: ${spring.cloud.nacos.password}
  store:
    # support: file 、 db 、 redis 、 raft
    mode: file
EOF

# 3. 启动 Seata Server
cd "$SEATA_SRC"
nohup mvn exec:java -Dexec.mainClass="org.apache.seata.server.ServerApplication" -pl server > /tmp/seata-server.log 2>&1 &
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
- 源码版本 2.8.0-SNAPSHOT 已修复此问题
- 启动命令是 `mvn exec:java`，不是 `spring-boot:run`

### 4. 安装依赖模块

部分模块依赖 `cloud-commons` 和 `cloud-sample-api`，启动前需先安装：
```bash
./mvnw -N install -q && ./mvnw -pl cloud-commons,cloud-sample-api install -DskipTests -q
```

## 启动方式

### 方式一：一键启动所有服务（推荐）

```bash
sh start-all.sh        # 启动所有服务
sh start-all.sh stop   # 停止所有服务
sh start-all.sh status # 查看服务状态
```

脚本会自动：检查 Nacos → 安装依赖模块（cloud-commons、cloud-sample-api） → 按顺序启动所有模块 → 执行验证 → 汇总结果。

### 方式二：逐个启动（按顺序）

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

快速刷新 `http://localhost:8764/consumer-sample/hi?name=hongxi` 触发限流时返回：
```json
{"code":444,"msg":"Sentinel gateway block"}
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

启动时指定模型：
```bash
./mvnw -pl cloud-ai-sample spring-boot:run -Dspring-boot.run.arguments=--spring.ai.openai.chat.options.model=qwen3.7-plus
```

支持的视觉模型示例：`qwen3.7-plus`、`qwen-vl-max`、`qwen-vl-plus` 等。

```bash
# 通过 URL 分析图片（神舟十号海报）
curl -X POST "http://localhost:8080/ai/vision/analyze-url" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg"

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
   # 在项目根目录下执行
   sh start-all.sh stop
   ```

1. **检查 RocketMQ 是否已安装**：
   ```bash
   # 检查 NameServer 端口是否就绪
   if nc -z 127.0.0.1 9876 2>/dev/null; then
     echo "✓ RocketMQ 已运行"
   else
     echo "✗ RocketMQ 未运行"
   fi
   ```

2. **如果 RocketMQ 已运行**，直接跳到步骤 4 创建 Topic 和 Consumer Group，然后启动 Stream 模块验证。

3. **如果 RocketMQ 未安装或未运行**，询问用户：
   > "Stream 模块依赖 RocketMQ，但当前未检测到运行中的 RocketMQ 服务。是否需要我帮您自动完成以下操作？
   > 1. 下载并安装 RocketMQ 5.5.0
   > 2. 启动 NameServer 和 Broker
   > 3. 创建所需的 Topic 和 Consumer Group
   > 4. 启动 Stream 模块并验证消息收发"

   **如果用户同意**，按以下步骤执行：

   #### 步骤 1：下载 RocketMQ
   ```bash
   cd /tmp
   curl -LO https://dist.apache.org/repos/dist/release/rocketmq/5.5.0/rocketmq-all-5.5.0-bin-release.zip
   unzip rocketmq-all-5.5.0-bin-release.zip
   mv rocketmq-all-5.5.0-bin-release ~/rocketmq
   cd ~/rocketmq
   ```

   #### 步骤 2：启动 NameServer 和 Broker
   ```bash
   # 后台启动 NameServer
   nohup bin/mqnamesrv > namesrv.log 2>&1 &
   echo "NameServer 启动中..."
   sleep 5

   # 后台启动 Broker
   nohup bin/mqbroker -n localhost:9876 > broker.log 2>&1 &
   echo "Broker 启动中..."
   sleep 10

   # 验证是否启动成功
   if nc -z 127.0.0.1 9876 2>/dev/null; then
     echo "✓ RocketMQ 已就绪"
   else
     echo "✗ RocketMQ 启动失败，请查看日志"
     return 1
   fi
   ```

4. **创建 Topic 和 Consumer Group**（无论是否新安装，都需要执行）：
   ```bash
   # 创建 stream-demo-topic 及其消费组
   bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic -a +message.type=NORMAL
   bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-demo-consumer-group

   # 创建 stream-demo-topic2 及其消费组
   bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic2 -a +message.type=NORMAL
   bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-demo-consumer-group2

   echo "✓ Topic 和 Consumer Group 创建完成"
   ```

5. **启动 Stream 模块并验证**：
   ```bash
   # 在项目根目录下执行
   ./mvnw -pl cloud-stream-sample spring-boot:run > logs/stream-sample.log 2>&1 &
   echo "Stream 模块启动中..."
   sleep 15

   # 验证消息收发
   echo "=== 验证消息消费 ==="
   grep "Received message: Hello" logs/stream-sample.log && echo "✓ stream-demo-topic 消息消费正常" || echo "✗ 未收到 Hello 消息"
   grep "收到消息: 你好" logs/stream-sample.log && echo "✓ stream-demo-topic2 消息消费正常" || echo "✗ 未收到 你好 消息"
   ```

6. **验证完成后停止 Stream 模块**：
   ```bash
   pkill -f "cloud-stream-sample" && echo "✓ Stream 模块已停止" || echo "Stream 模块未运行"
   ```

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

   **如果用户同意**，按以下步骤执行：

   #### 步骤 1：初始化数据库
   ```bash
   # 创建 seata 数据库并导入表结构
   mysql -u root -proot1234 -e "CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
   mysql -u root -proot1234 seata < cloud-seata-sample/all.sql
   echo "✓ 数据库初始化完成"
   ```

   #### 步骤 2：配置 Nacos（创建 seata.properties）
   启动 cloud-nacos-config-sample 用于发布配置：
   
```shell
CONTENT="service.vgroupMapping.default_tx_group=default
service.vgroupMapping.order-service-tx-group=default
service.vgroupMapping.account-service-tx-group=default
service.vgroupMapping.business-service-tx-group=default
service.vgroupMapping.storage-service-tx-group=default"

curl -X POST http://localhost:8761/nacos/publishConfig \
  -d "dataId=seata.properties" \
  -d "group=SEATA_GROUP" \
  -d "type=properties" \
  --data-urlencode "content=$CONTENT"

echo "✓ Nacos 配置创建完成"
```

   #### 步骤 3：检查 Seata Server 是否运行
   启动 cloud-nacos-discovery-sample 用于查询注册的实例：
   ```bash
   # 检查 Seata Server 是否在 Nacos 中注册
   SEATA_REGISTERED=$(curl -s "http://localhost:8760/discovery/instances/seata-server?group=SEATA_GROUP" 2>/dev/null | grep -c '"ip"')
   
   if [ "$SEATA_REGISTERED" -gt 0 ]; then
     echo "✓ Seata Server 已运行并在 Nacos 中注册"
   else
     echo "✗ Seata Server 未运行"
     echo "请执行前置条件中的「Seata Server 源码启动方式」脚本（含 clone、配置、启动）"
     return 1
   fi
   ```

   #### 步骤 4：启动 4 个微服务
   ```bash
   # 在项目根目录下执行

   # 按顺序启动服务
   echo "启动 storage-service (18082)..."
   ./mvnw -pl cloud-seata-sample/storage-service spring-boot:run > logs/seata-storage.log 2>&1 &
   sleep 10

   echo "启动 account-service (18084)..."
   ./mvnw -pl cloud-seata-sample/account-service spring-boot:run > logs/seata-account.log 2>&1 &
   sleep 10

   echo "启动 order-service (18083)..."
   ./mvnw -pl cloud-seata-sample/order-service spring-boot:run > logs/seata-order.log 2>&1 &
   sleep 10

   echo "启动 business-service (18081)..."
   ./mvnw -pl cloud-seata-sample/business-service spring-boot:run > logs/seata-business.log 2>&1 &
   sleep 15

   echo "✓ 所有服务启动完成"
   ```

   #### 步骤 5：验证分布式事务
   ```bash
   # 记录初始数据
   echo "=== 初始数据状态 ==="
   mysql -u root -proot1234 seata -e "
   SELECT '账户余额' AS 类型, money AS 当前值 FROM account_tbl WHERE user_id='U100001'
   UNION ALL
   SELECT '库存数量', count FROM storage_tbl WHERE commodity_code='C00321'
   UNION ALL
   SELECT '订单数量', COUNT(*) FROM order_tbl;
   "

   # 场景 1：验证事务回滚（mock 异常）
   # 接口会随机抛出异常，返回 500 表示触发了异常，此时事务应回滚
   echo "=== 场景 1：验证事务回滚（返回 500 表示 mock 异常触发）==="
   curl -s -o /dev/null -w "HTTP %{http_code}" http://127.0.0.1:18081/seata/rest
   echo ""

   # 场景 2：验证事务提交成功
   # 循环调用直到返回 SUCCESS（无异常），验证分布式事务正常提交
   echo ""
   echo "=== 场景 2：验证事务提交成功（循环调用直到成功）==="
   for i in $(seq 1 20); do
     result=$(curl -s -w "\n%{http_code}" http://127.0.0.1:18081/seata/rest)
     http_code=$(echo "$result" | tail -1)
     if [ "$http_code" = "200" ]; then
       echo "✓ 第 ${i} 次调用成功，事务已提交"
       break
     else
       echo "第 ${i} 次调用返回 ${http_code}（mock 异常，事务已回滚），继续重试..."
     fi
   done

   # 验证 Feign 方式（同样循环调用直到成功）
   echo ""
   echo "=== 验证 FeignClient 方式 ==="
   for i in $(seq 1 20); do
     result=$(curl -s -w "\n%{http_code}" http://127.0.0.1:18081/seata/feign)
     http_code=$(echo "$result" | tail -1)
     if [ "$http_code" = "200" ]; then
       echo "✓ 第 ${i} 次调用成功（Feign），事务已提交"
       break
     else
       echo "第 ${i} 次调用返回 ${http_code}（Feign，mock 异常），继续重试..."
     fi
   done

   # 验证 Xid 传递
   echo ""
   echo "=== 验证 Xid 传递 ==="
   grep -E "Begin.*xid:" logs/seata-storage.log | tail -1
   grep -E "Begin.*xid:" logs/seata-order.log | tail -1
   grep -E "Begin.*xid:" logs/seata-account.log | tail -1

   # 验证数据一致性
   echo ""
   echo "=== 验证数据一致性 ==="
   mysql -u root -proot1234 seata -e "
   SELECT '账户余额' AS 类型, money AS 当前值 FROM account_tbl WHERE user_id='U100001'
   UNION ALL
   SELECT '库存数量', count FROM storage_tbl WHERE commodity_code='C00321'
   UNION ALL
   SELECT '订单数量', COUNT(*) FROM order_tbl;
   "

   echo ""
   echo "预期结果："
   echo "- 用户余额：10000 = 当前余额 + 2(单价) × 订单数 × 2(每单数量)"
   echo "- 库存数量：100 = 当前库存 + 订单数 × 2(每单数量)"
   echo ""
   echo "说明："
   echo "- 返回 500 时：mock 异常触发，事务回滚，数据不变"
   echo "- 返回 200 时：事务正常提交，余额减少、库存减少、订单增加"
   ```

   #### 步骤 6：验证完成后停止所有 Seata 微服务和 Seata Server
   ```bash
   pkill -f "cloud-seata-sample" && echo "✓ Seata 微服务已停止" || echo "Seata 微服务未运行"
   pkill -f "incubator-seata" && echo "✓ Seata Server 已停止" || echo "Seata Server 未运行"
   ```

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
