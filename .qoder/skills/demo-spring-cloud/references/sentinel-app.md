# 🛡️ Sentinel 应用级熔断降级

> 🔴 **共 7 个步骤，必须逐一执行，不可跳过。每步执行后确认返回结果是否符合预期。**

## 前置条件

- `cloud-consumer-sample`（8766）、`cloud-provider-sample`（8765）、`cloud-nacos-config-sample`（8761）已启动
- 规则通过 Nacos 动态推送（group: `SENTINEL_GROUP`），无需重启服务

## 原理说明

`cloud-consumer-sample` 和 `cloud-provider-sample` 均集成了 Sentinel，通过 Nacos 动态推送规则：
- **限流**：`sentinel-spring-webmvc-v6x-adapter` 自动将 Controller 接口注册为 Sentinel 资源
- **熔断降级**：Feign / RestTemplate 调用下游服务时，通过 Sentinel 熔断规则保护出站调用
- **模拟下游不可用**：向 provider 推送 `count=0` 限流规则，使 provider 拒绝所有请求，无需杀进程

| 模块     | 数据源 | data-id                        | 规则类型          |
|--------|-----|--------------------------------|---------------|
| consumer | ds1 | `cloud.sample.consumer.flow`    | 限流（flow）      |
| consumer | ds2 | `cloud.sample.consumer.degrade` | 熔断降级（degrade） |
| provider | ds1 | `cloud.sample.provider.flow`    | 限流（flow）      |

---

## Step 1：推送限流规则（QPS=1）

```shell
curl -s --get --data-urlencode 'dataId=cloud.sample.consumer.flow' --data-urlencode 'group=SENTINEL_GROUP' --data-urlencode 'type=json' --data-urlencode 'content=[{"resource":"/hi","grade":1,"count":1}]' 'http://localhost:8761/nacos/publishConfig'
```

**预期结果**：返回 `true`

---

## Step 2：快速连续调用验证限流

```shell
curl 'http://localhost:8766/hi?name=test&version=2.0'
curl 'http://localhost:8766/hi?name=test&version=2.0'
```

**预期结果**：
- 第 1 次：正常返回 `Hi, test, Here is 8765`
- 第 2 次：返回 `Blocked by Sentinel (flow limiting)`

---

## Step 3：推送降级规则（异常比例 50%，熔断 10s）

```shell
curl -s --get --data-urlencode 'dataId=cloud.sample.consumer.degrade' --data-urlencode 'group=SENTINEL_GROUP' --data-urlencode 'type=json' --data-urlencode 'content=[{"resource":"GET:http://provider-sample/hello","grade":2,"count":0.5,"timeWindow":10,"minRequestAmount":1,"statIntervalMs":10000},{"resource":"GET:http://provider-sample","grade":2,"count":0.5,"timeWindow":10,"minRequestAmount":1,"statIntervalMs":10000}]' 'http://localhost:8761/nacos/publishConfig'
```

**预期结果**：返回 `true`

> **注意**：`grade=2`（异常比例）的 `count` 为比例阈值，`count=0.5` 表示 50% 异常即触发。`statIntervalMs=10000` 确保请求落在同一统计窗口内。

---

## Step 4：向 provider 推送限流规则（count=0，模拟下游不可用）

```shell
curl -s --get --data-urlencode 'dataId=cloud.sample.provider.flow' --data-urlencode 'group=SENTINEL_GROUP' --data-urlencode 'type=json' --data-urlencode 'content=[{"resource":"/hello","grade":1,"count":0}]' 'http://localhost:8761/nacos/publishConfig'
```

**预期结果**：返回 `true`

> **说明**：向 provider 推送 `count=0` 限流规则，使 provider 拒绝所有 `/hello` 请求。consumer 的 Feign/RestTemplate 调用会收到错误响应，从而触发熔断降级。**无需停止 provider 进程**，删除规则即可即时恢复。

---

## Step 5：调用 Feign 路径触发 fallback

```shell
curl 'http://localhost:8766/hi?name=test&version=2.0'
```

**预期结果**：返回 `fallback: service unavailable, name=test`

---

## Step 6：调用 RestTemplate 路径触发熔断

```shell
curl 'http://localhost:8766/hi?name=test&version=1.0'
curl 'http://localhost:8766/hi?name=test&version=1.0'
```

**预期结果**：
- 第 1 次：返回 `Blocked by Sentinel`（异常比例已达阈值，熔断拦截）
- 第 2 次：返回 `Blocked by Sentinel`（熔断持续）

---

## Step 7：清理规则，即时恢复

```shell
curl -s "http://localhost:8761/nacos/removeConfig?dataId=cloud.sample.consumer.flow&group=SENTINEL_GROUP"
curl -s "http://localhost:8761/nacos/removeConfig?dataId=cloud.sample.consumer.degrade&group=SENTINEL_GROUP"
curl -s "http://localhost:8761/nacos/removeConfig?dataId=cloud.sample.provider.flow&group=SENTINEL_GROUP"
```

验证恢复：

```shell
curl 'http://localhost:8766/hi?name=test&version=2.0'
curl 'http://localhost:8766/hi?name=test&version=1.0'
```

**预期结果**：均正常返回 `Hi, test, Here is 8765`

> 规则通过 Nacos 动态生效，删除后即时恢复，无需重启任何服务。
