# ☁️ Spring Cloud Alibaba Samples
> Spring Cloud 生态研究（Based on **Spring Boot 4.x** and **Spring Cloud Alibaba 2025.1.x**） <br>
> 🎯 以生产环境可参考为目标，打造一个完整的 Spring Cloud 示例项目。

### 🤖 AI 一键演示

> 本项目内置 Qoder Agent Skill，clone 后在 Qoder 中输入 `/demo-spring-cloud` 或告诉 AI "演示项目"，
> 即可自动完成环境检查、服务启动、接口验证全流程。无需手动操作。

```
# 快速体验（仅需 Nacos）
告诉 AI: "演示本项目"

# 单独验证某个场景
告诉 AI: "验证 Seata 分布式事务"
告诉 AI: "验证 Stream 消息收发"
告诉 AI: "演示 Spring AI"
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
| 📨 cloud-stream-sample           | stream            | -            | Spring Cloud Stream         |
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

#### 🏃 Run RocketMQ locally
download [rocketmq-all-5.5.0-bin-release.zip](https://dist.apache.org/repos/dist/release/rocketmq/5.5.0/rocketmq-all-5.5.0-bin-release.zip)
```shell
bin/mqnamesrv
bin/mqbroker -n localhost:9876
```

#### 📝 Create Topic and Consumer Group
```shell
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-demo-consumer-group
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic2 -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-demo-consumer-group2
```

#### 🏃 Run Demo
启动`stream`，观察日志 <br>
命令查看消费组的消费进度
```shell
bin/mqadmin consumerProgress -n localhost:9876 -g stream-demo-consumer-group2
```

### 🔄 Seata 分布式事务演示

前置条件：MySQL + Seata Server，请参考 [SKILL.md](.qoder/skills/demo-spring-cloud/SKILL.md) 中的环境准备步骤。

启动 4 个微服务（business 18081、storage 18082、order 18083、account 18084），验证分布式事务的回滚与提交。

### 🌿 分支说明
- 🌱 `springboot3`: 基于 Spring Boot 3.5.0+ 的示例
- 🌿 `eureka`: 初始版本，使用 Eureka 作为注册中心

&copy; [hongxi.org](http://hongxi.org)
