# Spring Cloud Alibaba Samples
Spring Cloud 本身并不是一个开箱即用的框架，而是一套微服务开发的规范标准，Alibaba的实现很好。

### 演示
首先安装部署nacos，请参考 nacos.io

依次启动provider-reactive,consumer-reactive,gateway <br>
1. 直接访问 localhost:8763/hi?name=hongxi
1. 通过网关访问 localhost:8764/demo-consumer-reactive/hi?name=hongxi

接着启动provider,consumer <br>
1. 直接访问 localhost:8766/hi?name=hongxi
1. 通过网关访问 localhost:8764/demo-consumer/hi?name=hongxi

&copy; [hongxi.org](http://hongxi.org)
