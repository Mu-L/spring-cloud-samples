# ☁️ Spring Cloud Alibaba Samples
> Spring Cloud Ecosystem Research (Based on **Spring Boot 4.x** and **Spring Cloud Alibaba 2025.1.x**) <br>
> 🎯 Aiming to be a production-ready reference, building a comprehensive Spring Cloud sample project.

### 📦 Module Overview
| Module                             | Alias             | Port           | Description                     |
|------------------------------------|-------------------|----------------|---------------------------------|
| 🌐 cloud-gateway-sample            | gateway           | 8764           | Spring Cloud Gateway            |
| 📥 cloud-consumer-sample           | consumer          | 8766           | Web Consumer                    |
| 📤 cloud-provider-sample           | provider          | 8765           | Web Provider                    |
| ⚡ cloud-consumer-reactive-sample  | consumer-reactive | 8763           | Reactive Web Consumer           |
| ⚡ cloud-provider-reactive-sample  | provider-reactive | 8762           | Reactive Web Provider           |
| 🔗 cloud-provider-dubbo-sample     | provider-dubbo    | 50051          | Dubbo Provider                  |
| 🔗 cloud-consumer-dubbo-sample     | consumer-dubbo    | -              | Dubbo Consumer                  |
| 📋 cloud-sample-api                | api               | -              | Interface & Proto               |
| ⚙️ cloud-nacos-config-sample       | config            | 8761           | Nacos Config                    |
| 🔍 cloud-nacos-discovery-sample    | discovery         | 8760           | Nacos Discovery                 |
| 📨 cloud-stream-sample             | stream            | -              | Spring Cloud Stream             |
| 🔌 cloud-grpc-server-sample        | grpc-server       | 9090<br>8090   | gRPC Server<br>(8090 is Web port) |
| 🔌 cloud-grpc-client-sample        | grpc-client       | -              | gRPC Client                     |
| 🤖 cloud-ai-sample                 | ai                | 8080           | Spring AI                       |
| 🔄 cloud-seata-sample              | seata             | -              | Apache Seata                    |
| 🧩 cloud-commons                   | commons           | -              | Cloud Commons                   |

<picture>
  <source srcset="arch.svg" type="image/svg+xml">
  <img src="arch.png" alt="Architecture">
</picture>

### 🔍 Service Registration & Discovery Demo
> First, install and deploy Nacos, then set environment variables
```shell
export SPRING_CLOUD_NACOS_USERNAME=your_username
export SPRING_CLOUD_NACOS_PASSWORD=your_password
```

#### 🟢 Nacos Discovery Demo
Start the discovery module, then access the following endpoint
```shell
curl http://localhost:8760/discovery/instances
```

#### 🌐 Standard Web Service Registration & Discovery
Start provider, consumer, and gateway in order <br>
Direct access (consumer → provider)
```shell
curl 'http://localhost:8766/hi?name=hongxi'
```
Access via gateway (gateway → consumer → provider)
```shell
curl 'http://localhost:8764/consumer-sample/hi?name=hongxi'
```

#### ⚡ Reactive Web Service Registration & Discovery
Then start provider-reactive and consumer-reactive <br>
Direct access (consumer-reactive → provider-reactive)
```shell
curl 'http://localhost:8763/hi?name=hongxi'
```
Access via gateway (gateway → consumer-reactive → provider-reactive)
```shell
curl 'http://localhost:8764/consumer-reactive-sample/hi?name=hongxi'
```

#### 🔗 Dubbo Service Registration & Discovery
Then start provider-dubbo <br>
Direct access (consumer → provider-dubbo)
```shell
curl 'http://localhost:8766/dubbo?name=hongxi'
```
Access via gateway (gateway → consumer → provider-dubbo)
```shell
curl 'http://localhost:8764/consumer-sample/dubbo?name=hongxi'
```
Direct access (consumer-reactive → provider-dubbo)
```shell
curl 'http://localhost:8763/dubbo?name=hongxi'
```
Access via gateway (gateway → consumer-reactive → provider-dubbo)
```shell
curl 'http://localhost:8764/consumer-reactive-sample/dubbo?name=hongxi'
```

#### 🔌 gRPC Service Registration & Discovery
Leveraging Spring Cloud's service registration capability, include the `discovery` and `webmvc` dependencies. <br>
Also, you need to set the port registered to the registry; otherwise, `server.port` is used by default.
```yaml
server:
  port: 8090 # Web port
spring:
  cloud:
    nacos:
      discovery:
        port: ${spring.grpc.server.port} # Port registered to the registry
  grpc:
    server:
      port: 9090 # gRPC port
```
Regarding service discovery, Spring Cloud and gRPC use two different service discovery mechanisms. <br>
This project bridges DiscoveryClient via the NameResolver SPI to integrate both discovery modes. <br>
Please refer to `cloud-commons` and `grpc-client-sample` for the implementation details. <br>
Continuing from above, start grpc-server <br>
Direct access (consumer → grpc-server)
```shell
curl 'http://localhost:8766/grpc?name=hongxi'
```
Access via gateway (gateway → consumer → grpc-server)
```shell
curl 'http://localhost:8764/consumer-sample/grpc?name=hongxi'
```

#### 🎯 Pure Dubbo Provider/Consumer Demo
Start provider-dubbo and consumer-dubbo, then observe the logs

#### 🎯 Pure gRPC Server/Client Demo
Start grpc-server and grpc-client, then observe the logs

#### 🌐 Dubbo REST Demo
Start provider-dubbo and gateway <br>
Direct access to `dubbo rest` endpoints
```shell
curl http://localhost:50051/api/hello/lily
curl 'http://localhost:50051/api/add?a=1&b=2'
curl -X POST http://localhost:50051/api/echo -H "Content-Type: application/json" -d '{"message":"hi"}'
curl 'http://localhost:50051/api/greet/lily?lang=zh'
```
Access `dubbo rest` endpoints via gateway (gateway → provider-dubbo)
```shell
curl http://localhost:8764/provider-dubbo-sample/api/hello/lily
curl 'http://localhost:8764/provider-dubbo-sample/api/add?a=1&b=2'
curl -X POST http://localhost:8764/provider-dubbo-sample/api/echo -H "Content-Type: application/json" -d '{"message":"hi"}'
curl 'http://localhost:8764/provider-dubbo-sample/api/greet/lily?lang=zh'
```

### 🚀 Script Demo

> Use the script to verify all of the following at once:
1. Nacos Discovery verification
2. Standard Web service registration & discovery
3. Reactive Web service registration & discovery
4. Dubbo service registration & discovery
5. gRPC service registration & discovery
6. Pure Dubbo provider/consumer verification
7. Pure gRPC server/client verification
8. Dubbo REST endpoint verification
9. Nacos Config verification
10. Summary of verification results

Start all services
```shell
sh start-all.sh
```
Stop all services
```shell
sh start-all.sh stop
```

### 🛡️ Sentinel Gateway Demo
`cloud-gateway-sample` integrates Sentinel with Nacos-based rule configuration. Rule example: <br>
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
Demo: Quickly refresh the following endpoint in your browser several times
```text
http://localhost:8764/consumer-sample/hi?name=hongxi
```
When rate limiting is triggered, the response will be
```json
{"code":444,"msg":"Sentinel gateway block"}
```

### 📨 Stream Demo

#### 🏃 Run RocketMQ Locally
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
Start the `stream` module and observe the logs <br>
Check consumer group consumption progress via command
```shell
bin/mqadmin consumerProgress -n localhost:9876 -g stream-demo-consumer-group2
```

### 🌿 Branch Info
- 🌱 `springboot3`: Examples based on Spring Boot 3.5.0+
- 🌿 `eureka`: Initial version using Eureka as the registry

&copy; [hongxi.org](http://hongxi.org)
