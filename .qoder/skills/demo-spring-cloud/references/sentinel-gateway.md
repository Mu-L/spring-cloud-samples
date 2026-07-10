# 🛡️ Sentinel Gateway 限流演示

> 🔴 **共 5 个步骤，必须逐一执行，不可跳过。每步执行后确认返回结果是否符合预期。**

**前提**：`cloud-gateway-sample`（8764）、`cloud-consumer-sample`（8766）、`cloud-provider-sample`（8765）、`cloud-nacos-config-sample`（8761）已启动。

按以下步骤验证 Sentinel 网关限流：

## Step 1：发布 API 分组配置到 Nacos（group=SENTINEL_GROUP）

```shell
API_GROUP_JSON='[
  {"apiName":"consumer_reactive_api","predicateItems":[{"pattern":"/consumer-reactive-sample/**","matchStrategy":1}]},
  {"apiName":"consumer_api","predicateItems":[{"pattern":"/consumer-sample/**","matchStrategy":1}]}
]'
curl -s -X POST http://localhost:8761/nacos/publishConfig \
  --data-urlencode 'dataId=cloud.sample.gateway.gw-api-group' \
  --data-urlencode 'group=SENTINEL_GROUP' \
  --data-urlencode 'type=json' \
  --data-urlencode "content=$API_GROUP_JSON"
```

## Step 2：发布限流规则到 Nacos

```shell
FLOW_JSON='[
  {"resource":"consumer_reactive_api","resourceMode":1,"count":10},
  {"resource":"consumer_api","resourceMode":1,"count":5},
  {"resource":"consumer-reactive-sample","resourceMode":0,"count":20}
]'
curl -s -X POST http://localhost:8761/nacos/publishConfig \
  --data-urlencode 'dataId=cloud.sample.gateway.gw-flow' \
  --data-urlencode 'group=SENTINEL_GROUP' \
  --data-urlencode 'type=json' \
  --data-urlencode "content=$FLOW_JSON"
```

## Step 3：等待配置同步和 Gateway 加载规则

```shell
sleep 5
```

## Step 4：触发限流验证

consumer_api 阈值: 5 QPS，快速发 10 次请求：

```shell
for i in $(seq 1 10); do
  RESP=$(curl -s "http://localhost:8764/consumer-sample/hi?name=hongxi")
  echo "请求 $i: $RESP"
done
```

预期结果：前 5 次正常返回，后续请求被 Sentinel 拦截（返回 444 或错误信息），证明网关限流生效。

## Step 5：清理 Sentinel 配置

```shell
curl -s "http://localhost:8761/nacos/removeConfig?dataId=cloud.sample.gateway.gw-api-group&group=SENTINEL_GROUP"
curl -s "http://localhost:8761/nacos/removeConfig?dataId=cloud.sample.gateway.gw-flow&group=SENTINEL_GROUP"
```
