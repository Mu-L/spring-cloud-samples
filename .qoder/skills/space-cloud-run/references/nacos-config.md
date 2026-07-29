# ⚙️ Nacos Config 动态配置

> 🔴 **共 8 个步骤，必须逐一执行，不可跳过。每步执行后确认返回结果是否符合预期。**

## 前置条件

`cloud-nacos-config-sample`（端口 8761）已启动。

---

## Step 1：发布配置

```shell
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
```

**预期结果**：返回 `true`

---

## Step 2：获取配置

```shell
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
```

**预期结果**：返回 `wuhan`

---

## Step 3：监听配置变更

```shell
curl 'http://localhost:8761/nacos/listener?dataId=my.city'
```

**预期结果**：返回 `Add Lister successfully!`

---

## Step 4：删除配置

```shell
curl 'http://localhost:8761/nacos/removeConfig?dataId=my.city'
```

**预期结果**：返回 `true`

---

## Step 5：验证 @NacosConfig 注解注入

先发布配置：
```shell
curl 'http://localhost:8761/nacos/publishConfig?dataId=github.username&content=javahongxi'
```

再访问接口验证：
```shell
curl http://localhost:8761/config/hello
```

**预期结果**：返回 `Hello, javahongxi`

---

## Step 6：验证 @ConfigurationProperties Bean 绑定

发布 Properties 格式配置：
```shell
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
```

查看绑定结果：
```shell
curl http://localhost:8761/config/agent
```

**预期结果**：返回 JSON，如 `{"credits":2000000,"enabled":true,"name":"Trae CN","provider":{"apiKey":"xxx123aa","model":"Qwen3.7 Plus","name":"Alibaba"},"version":"3.3.60"}`

---

## Step 7：验证 @Value + @RefreshScope 注入

```shell
curl http://localhost:8761/config/value
```

**预期结果**：返回 `name:Trae CN,credits:2000000,enabled:true,model:Qwen3.7 Plus`

---

## Step 8：动态刷新验证

修改 github.username 配置：
```shell
curl 'http://localhost:8761/nacos/publishConfig?dataId=github.username&content=javahongxi-new'
```

再次访问验证：
```shell
curl http://localhost:8761/config/hello
```

**预期结果**：返回 `Hello, javahongxi-new`（值已动态刷新）

---

## 清理

```shell
curl 'http://localhost:8761/nacos/removeConfig?dataId=github.username'
curl 'http://localhost:8761/nacos/removeConfig?dataId=cloud-agent.properties'
```
