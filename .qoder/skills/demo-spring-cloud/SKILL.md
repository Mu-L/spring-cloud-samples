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

### 3. 安装 API 模块

部分模块依赖 `cloud-sample-api`，启动前需先安装：
```bash
./mvnw -N install -q && ./mvnw -pl cloud-sample-api install -DskipTests -q
```

## 启动方式

### 方式一：一键启动所有服务（推荐）

```bash
sh start-all.sh        # 启动所有服务
sh start-all.sh stop   # 停止所有服务
sh start-all.sh status # 查看服务状态
```

脚本会自动：检查 Nacos → 安装 API → 按顺序启动所有模块 → 执行验证 → 汇总结果。

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

常用演示接口：
```bash
# 简单聊天
curl "http://localhost:8080/ai/chat?message=你好"
# 流式输出
curl "http://localhost:8080/ai/chat/stream?message=讲一个故事"
# 结构化输出
curl "http://localhost:8080/ai/extract?text=张三今年25岁，是软件工程师"
# Tool Calling
curl "http://localhost:8080/ai/tool/weather?question=北京今天天气怎么样？"
# ReAct Agent
curl "http://localhost:8080/ai/agent/chat?question=北京天气怎么样？适合出门吗？"
# MCP Server（SSE 端点）
# 连接地址: http://localhost:8080/sse
```

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

## 分支说明

- `springboot3`：基于 Spring Boot 3.5.0+ 的示例
- `eureka`：使用 Eureka 作为注册中心的初始版本
