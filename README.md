# Spring Cloud Alibaba Samples
Spring Cloud 生态研究（Based on **Spring Boot 4.x** and **Spring Cloud Alibaba 2025.1.x**） <br>
以生产环境可参考为目标，打造一个完整的 Spring Cloud 示例项目。

### 模块介绍
| 模块                             | 简称                | 端口    | 说明                    |
|--------------------------------|-------------------|-------|-----------------------|
| cloud-gateway-sample           | gateway           | 8764  | Spring Cloud Gateway  |
| cloud-consumer-sample          | consumer          | 8766  | Web Consumer          |
| cloud-provider-sample          | provider          | 8765  | Web Provider          |
| cloud-consumer-reactive-sample | consumer-reactive | 8763  | Reactive Web Consumer |
| cloud-provider-reactive-sample | provider-reactive | 8762  | Reactive Web Provider |
| cloud-provider-dubbo-sample    | provider-dubbo    | 50051 | Dubbo Provider        |
| cloud-consumer-dubbo-sample    | consumer-dubbo    | -     | Dubbo Consumer        |
| cloud-sample-api               | api               | -     | interface             |
| cloud-nacos-config-sample      | config            | 8761  | Nacos Config          |
| cloud-sentinel-sample          | sentinel          | 8767  | Sentinel              |
| cloud-sentinel-gateway-sample  | sentinel-gateway  | 8768  | Gateway with Sentinel |
| cloud-stream-sample            | stream            | -     | Spring Cloud Stream   |
| cloud-rocketmq-sample          | rocketmq-consumer | -     | RocketMQ Consumer     |

<picture>
  <source srcset="arch.svg" type="image/svg+xml">
  <img src="arch.png" alt="架构图">
</picture>

### 服务注册与发现演示
首先安装部署nacos，请参考 nacos.io

#### 普通Web服务的注册与发现
依次启动provider,consumer,gateway <br>
直接访问(consumer → provider)
```shell
curl 'http://localhost:8766/hi?name=hongxi'
```
通过网关访问(gateway → consumer → provider)
```shell
curl 'http://localhost:8764/consumer-sample/hi?name=hongxi'
```

#### Reactive Web 服务注册与发现
接着启动provider-reactive,consumer-reactive <br>
直接访问(consumer-reactive → provider-reactive)
```shell
curl 'http://localhost:8763/hi?name=hongxi'
```
通过网关访问(gateway → consumer-reactive → provider-reactive)
```shell
curl 'http://localhost:8764/consumer-reactive-sample/hi?name=hongxi'
```

#### dubbo 服务注册与发现
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

### 脚本演示
启动所有服务（脚本最后会执行curl并输出响应结果）
```shell
sh start-all.sh
```
停止所有服务
```shell
sh start-all.sh stop
```

### 网关访问dubbo演示
启动provider-dubbo,gateway<br>
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

### 带哨兵的网关演示
执行`start-all.sh`后手动启动 sentinel-gateway
```shell
curl 'http://localhost:8768/consumer-sample/hi?name=hongxi'
curl 'http://localhost:8768/consumer-reactive-sample/hi?name=hongxi'
curl http://localhost:8768/gateway
```
第一个url快速访问几次会触发限流

### RocketMQ 相关演示
#### Run RocketMQ locally
download [rocketmq-all-5.5.0-bin-release.zip](https://dist.apache.org/repos/dist/release/rocketmq/5.5.0/rocketmq-all-5.5.0-bin-release.zip)
```shell
bin/mqnamesrv
bin/mqbroker -n localhost:9876 --enable-proxy
```

#### Create Topic and Consumer Group
```shell
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t demo-normal-topic -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g my-consumer_demo-normal-topic
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-demo-consumer-group
```

#### Run Demo
1. 启动`rocketmq-consumer` `provider-dubbo` `consumer-dubbo`
2. 观察`consumer-dubbo`日志
3. 启动`stream`，观察日志

### 分支说明
- branch springboot3: 基于 Spring Boot 3.5.0+ 的示例
- branch eureka: 初始版本，使用eureka作为注册中心

&copy; [hongxi.org](http://hongxi.org)
