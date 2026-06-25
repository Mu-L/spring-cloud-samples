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

```bash
bin/mqnamesrv
bin/mqbroker -n localhost:9876 --enable-proxy
```

### 3. 安装依赖模块

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
| cloud-stream-sample | - | 需先启动 RocketMQ |
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

### 7. Nacos Config 动态配置

```bash
# 发布配置
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
# 获取配置
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
```

### 8. Sentinel 网关限流

快速刷新 `http://localhost:8764/consumer-sample/hi?name=hongxi` 触发限流时返回：
```json
{"code":444,"msg":"Sentinel gateway block"}
```

### 9. Spring AI 模块

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

### 10. 纯 Dubbo / gRPC 演示

启动后观察日志即可：
- `cloud-consumer-dubbo-sample`：日志中出现 `Hello, lily` 表示调用成功
- `cloud-grpc-client-sample`：日志中出现 `Hello, lily` 表示调用成功

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
