# Spring Cloud Alibaba Samples
Spring Cloud 本身并不是一个开箱即用的框架，而是一套微服务开发的规范标准，Alibaba的实现很好。

### 模块介绍
模块 | 简称                | 说明
------ |-------------------| ------
cloud-gateway-sample | gateway           | spring cloud gateway
cloud-consumer-sample | consumer          | web consumer
cloud-provider-sample | provider          | web provider
cloud-consumer-reactive-sample | consumer-reactive | webflux consumer
cloud-provider-reactive-sample | provider-reactive | webflux provider    
cloud-provider-dubbo-sample | provider-dubbo    | dubbo provider
cloud-sample-api | api               | interface api
cloud-nacos-config-sample | config            | nacos config

### 演示
首先安装部署nacos，请参考 nacos.io

依次启动provider-reactive,consumer-reactive,gateway <br>
1. 直接访问 localhost:8763/hi?name=hongxi
1. 通过网关访问 localhost:8764/demo-consumer-reactive/hi?name=hongxi

接着启动provider,consumer <br>
1. 直接访问 localhost:8766/hi?name=hongxi
1. 通过网关访问 localhost:8764/demo-consumer/hi?name=hongxi

接着启动provider-dubbo <br>
1. 直接访问 localhost:8766/dubbo?name=hongxi
1. 通过网关访问 localhost:8764/demo-consumer/dubbo?name=hongxi
1. 直接访问 localhost:8763/dubbo?name=hongxi
1. 通过网关访问 localhost:8764/demo-consumer-reactive/dubbo?name=hongxi

### 脚本演示
```shell
# 启动所有服务（脚本最后会执行curl并输出响应结果）
sh start-all.sh
# 停止所有服务
sh start-all.sh stop
```

&copy; [hongxi.org](http://hongxi.org)
