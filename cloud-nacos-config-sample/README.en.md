## Nacos Config Demo

### Nacos Annotation Demo
Create a configuration in the Nacos console <br>
dataId: github.username <br>
content: javahongxi
```shell
curl http://localhost:8761/config/hello
```
1. Modify the configuration and access the endpoint again
2. Delete the configuration and observe the logs

### Bean Configuration and @Value Annotation Demo
Create a configuration in the Nacos console <br>
dataId: cloud-agent.properties <br>
content: (Properties format)
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
Modify the configuration and access the endpoints again

### Nacos Native API Demo
```shell
curl 'http://localhost:8761/nacos/listener?dataId=my.city'
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
curl 'http://localhost:8761/nacos/removeConfig?dataId=my.city'
```
