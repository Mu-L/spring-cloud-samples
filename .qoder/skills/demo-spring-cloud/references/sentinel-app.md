# 🛡️ Sentinel 应用级熔断降级

`cloud-consumer-sample` 集成了 Sentinel，通过 Nacos 动态推送规则，演示两类场景：
- **限流**：`sentinel-spring-webmvc-v6x-adapter` 自动将 Controller 接口（如 `/hi`）注册为 Sentinel 资源，推送 flow 规则即可限制入口 QPS
- **熔断降级**：Feign / RestTemplate 调用下游服务时，通过 Sentinel 熔断规则保护出站调用，异常时走 fallback

| 场景              | 资源名                                             | 规则类型    | 说明                   |
|-----------------|-------------------------------------------------|---------|----------------------|
| 接口限流            | `/hi`（URI 自动注册）                                 | flow    | 限制 consumer 自身接口 QPS |
| Feign 熔断        | `GET:http://provider-sample/hello` （Feign 自动生成） | degrade | 下游异常时触发 fallback     |
| RestTemplate 熔断 | `GET:http://provider-sample`（urlCleaner 去端口后）   | degrade | 下游异常时触发 fallback     |

规则通过 Nacos 数据源动态推送（无需重启），group: `SENTINEL_GROUP`

| 数据源 | data-id                         | 规则类型          |
|-----|---------------------------------|---------------|
| ds1 | `cloud.sample.consumer.flow`    | 限流（flow）      |
| ds2 | `cloud.sample.consumer.degrade` | 熔断降级（degrade） |

前置条件：启动 `cloud-nacos-config-sample`（端口 8761），通过其 `/nacos/publishConfig` 接口推送规则。

## 演示限流（consumer 自身接口）

1. 推送限流规则，资源名 `/hi`，QPS 阈值 = 1：
```shell
curl -s --get --data-urlencode 'dataId=cloud.sample.consumer.flow' --data-urlencode 'group=SENTINEL_GROUP' --data-urlencode 'type=json' --data-urlencode 'content=[{"resource":"/hi","grade":1,"count":1}]' 'http://localhost:8761/nacos/publishConfig'
```

2. 快速连续调用，第二次请求被限流：
```shell
curl 'http://localhost:8766/hi?name=test&version=2.0'
curl 'http://localhost:8766/hi?name=test&version=2.0'
# 第二次返回: Blocked by Sentinel
```

## 演示熔断降级（Feign + RestTemplate 出站调用）

`SentinelProtectInterceptor` 为 RestTemplate 创建两个资源：`hostResource`（不含路径）和 `hostWithPathResource`（含路径），`urlCleaner` 会去除端口号使资源名与 Feign 保持一致。降级规则需同时覆盖两个资源名。

1. 推送降级规则（异常比例策略，阈值 50%，熔断 10 秒）：
```shell
curl -s --get --data-urlencode 'dataId=cloud.sample.consumer.degrade' --data-urlencode 'group=SENTINEL_GROUP' --data-urlencode 'type=json' --data-urlencode 'content=[{"resource":"GET:http://provider-sample/hello","grade":2,"count":0.5,"timeWindow":10,"minRequestAmount":1,"statIntervalMs":10000},{"resource":"GET:http://provider-sample","grade":2,"count":0.5,"timeWindow":10,"minRequestAmount":1,"statIntervalMs":10000}]' 'http://localhost:8761/nacos/publishConfig'
```

> **注意**：`grade=2`（异常比例）的 `count` 为比例阈值，判断条件为 `currentRatio > count`，因此 `count` 不能设为 1（需要 >100% 异常，不可能触发），应设为 0.5（50% 异常即触发）。`statIntervalMs` 需足够大（如 10000ms），确保请求落在同一统计窗口内。

2. 停止 provider-sample，使下游不可用：
```shell
kill -9 $(cat .pids/provider.pid)
```

3. 调用 Feign 路径触发 fallback：
```shell
curl 'http://localhost:8766/hi?name=test&version=2.0'
# 返回: fallback: service unavailable, name=test
```

4. 调用 RestTemplate 路径触发 fallback（第一次请求记录异常，第二次请求被熔断拦截）：
```shell
curl 'http://localhost:8766/hi?name=test&version=1.0'  # 第一次：500（异常被 Sentinel 记录）
curl 'http://localhost:8766/hi?name=test&version=1.0'  # 第二次：Blocked by Sentinel
```

> 规则通过 Nacos 动态生效，无需重启服务。恢复 provider 并等待熔断窗口过期后自动恢复正常。
