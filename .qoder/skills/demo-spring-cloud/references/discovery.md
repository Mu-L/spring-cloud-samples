# 🔍 服务注册与发现演示

> 🔴 **共 12 个步骤，必须逐一执行，不可跳过任何一步。**

## 前置条件

Nacos 已启动并已切换为免密模式（`nacos.core.auth.enabled=false`）。

已启动模块：nacos-discovery(8760)、nacos-config(8761)、gateway(8764)、provider(8765)、consumer(8766)、provider-reactive(8762)、consumer-reactive(8763)、provider-dubbo(50051)、grpc-server(8090/9090)

---

## Step 1：查看已注册服务列表

```shell
curl http://localhost:8760/discovery/instances
```

**预期结果**：返回已注册的服务列表 JSON，包含上述已启动的模块名称。

---

## Step 2：Web 直接调用（consumer → provider）

```shell
curl 'http://localhost:8766/hi?name=hongxi'
```

**预期结果**：返回 `Hi, hongxi, Here is 8765`（consumer 通过负载均衡调用 provider）

---

## Step 3：Web 网关调用（gateway → consumer → provider）

```shell
curl 'http://localhost:8764/consumer-sample/hi?name=hongxi'
```

**预期结果**：返回 `Hi, hongxi, Here is 8765`（经网关路由到 consumer 再到 provider）

---

## Step 4：Reactive 直接调用（consumer-reactive → provider-reactive）

```shell
curl 'http://localhost:8763/hi?name=hongxi'
```

**预期结果**：返回 `Hi, hongxi, Here is 8762`

---

## Step 5：Reactive 网关调用（gateway → consumer-reactive → provider-reactive）

```shell
curl 'http://localhost:8764/consumer-reactive-sample/hi?name=hongxi'
```

**预期结果**：返回 `Hi, hongxi, Here is 8762`

---

## Step 6：Dubbo 调用（consumer → provider-dubbo）

```shell
curl 'http://localhost:8766/dubbo?name=hongxi'
```

**预期结果**：返回包含 `hongxi` 的 Dubbo 调用结果

---

## Step 7：Dubbo 网关调用（gateway → consumer → provider-dubbo）

```shell
curl 'http://localhost:8764/consumer-sample/dubbo?name=hongxi'
```

**预期结果**：返回包含 `hongxi` 的 Dubbo 调用结果

---

## Step 8：Reactive → Dubbo 调用（consumer-reactive → provider-dubbo）

```shell
curl 'http://localhost:8763/dubbo?name=hongxi'
```

**预期结果**：返回包含 `hongxi` 的 Dubbo 调用结果

---

## Step 9：Reactive → Dubbo 网关调用（gateway → consumer-reactive → provider-dubbo）

```shell
curl 'http://localhost:8764/consumer-reactive-sample/dubbo?name=hongxi'
```

**预期结果**：返回包含 `hongxi` 的 Dubbo 调用结果

---

## Step 10：gRPC 调用（consumer → grpc-server）

```shell
curl 'http://localhost:8766/grpc?name=hongxi'
```

**预期结果**：返回 gRPC 调用结果，包含 `hongxi`

> gRPC 服务发现说明：Spring Cloud 与 gRPC 是两套服务发现模式，本项目通过 NameResolver SPI 桥接 DiscoveryClient 实现集成，具体参考 `cloud-commons` 模块。

---

## Step 11：gRPC 网关调用（gateway → consumer → grpc-server）

```shell
curl 'http://localhost:8764/consumer-sample/grpc?name=hongxi'
```

**预期结果**：返回 gRPC 调用结果，包含 `hongxi`

---

## Step 12：Dubbo REST 服务发现

直接访问 Dubbo REST 接口：
```shell
curl http://localhost:50051/api/hello/lily
curl 'http://localhost:50051/api/add?a=1&b=2'
curl -X POST http://localhost:50051/api/echo -H "Content-Type: application/json" -d '{"message":"hi"}'
curl 'http://localhost:50051/api/greet/lily?lang=zh'
```

通过网关访问 Dubbo REST 接口：
```shell
curl http://localhost:8764/provider-dubbo-sample/api/hello/lily
curl 'http://localhost:8764/provider-dubbo-sample/api/add?a=1&b=2'
curl -X POST http://localhost:8764/provider-dubbo-sample/api/echo -H "Content-Type: application/json" -d '{"message":"hi"}'
curl 'http://localhost:8764/provider-dubbo-sample/api/greet/lily?lang=zh'
```

**预期结果**：直接调用和网关调用返回相同结果，证明 Dubbo REST 服务注册正常。
