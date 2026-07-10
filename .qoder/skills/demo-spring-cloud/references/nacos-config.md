# ⚙️ Nacos Config 动态配置

**前提**：`cloud-nacos-config-sample`（8761）已启动。

按以下步骤逐一验证 Nacos Config 的三大核心能力：基础配置管理、@NacosConfig 注解注入、@ConfigurationProperties + @Value 动态刷新。

## Nacos 原生 API

通过 `cloud-nacos-config-sample` 模块（端口 8761）提供的接口管理配置，避免直接调用 Nacos API 的鉴权问题。

```shell
# 发布配置
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
# 获取配置
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
# 监听配置变更
curl 'http://localhost:8761/nacos/listener?dataId=my.city'
# 删除配置
curl 'http://localhost:8761/nacos/removeConfig?dataId=my.city'
```

## 演示 @NacosConfig 注解

先用原生 API 发布配置，再访问接口验证：

```shell
# 发布配置：dataId=github.username, content=javahongxi
curl 'http://localhost:8761/nacos/publishConfig?dataId=github.username&content=javahongxi'
# 访问（@NacosConfig 注入字段值）
curl http://localhost:8761/config/hello
# 修改配置后再访问，观察动态刷新
# 删除配置后观察日志（@NacosConfigListener 回调）
```

## 演示 @ConfigurationProperties 和 @Value

先用原生 API 发布 Properties 格式配置，再访问接口验证：

```shell
# 发布配置（Properties 格式）
CONTENT="cloud.agent.name=Trae CN
cloud.agent.version=3.3.60
cloud.agent.credits=2000000
cloud.agent.enabled=true
cloud.agent.provider.name=Alibaba
cloud.agent.provider.model=Qwen3.7 Plus
cloud.agent.provider.api-key=xxx123aa"
curl -s -X POST http://localhost:8761/nacos/publishConfig \
  -d "dataId=cloud-agent.properties" \
  -d "type=properties" \
  --data-urlencode "content=$CONTENT"
# 查看 @ConfigurationProperties Bean 绑定结果
curl http://localhost:8761/config/agent
# 查看 @Value + @RefreshScope 注入结果
curl http://localhost:8761/config/value
# 修改配置后再访问，观察动态刷新
```
