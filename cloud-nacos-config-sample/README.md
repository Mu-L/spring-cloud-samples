## Nacos Config 演示

### 演示Nacos注解
nacos console 创建配置<br>
dataId: github.username<br>
content: javahongxi
```shell
curl http://localhost:8761/config/hello
```
1. 修改配置后再访问
1. 删除配置后观察日志

### 演示Bean配置和Value注解
nacos console 创建配置<br>
dataId: cloud-agent.properties<br>
content: (Properties 格式)
```properties
cloud.agent.name=Trae CN
cloud.agent.version=3.3.60
cloud.agent.credits=2000000
cloud.agent.enabled=true
cloud.agent.provider.name=Alibaba
cloud.agent.provider.model=Qwen3.7 Plus
cloud.agent.provider.api-key=xxx123aa
```
```shell
curl http://localhost:8761/config/agent
```
```shell
curl http://localhost:8761/config/value
```
修改配置后再访问

### 演示Nacos原生API
```shell
curl 'http://localhost:8761/nacos/listener?dataId=my.city'
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
curl 'http://localhost:8761/nacos/removeConfig?dataId=my.city'
```
