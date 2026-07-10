# 🔍 Trace 链路追踪

> 🔴 **必须执行验证脚本，不可跳过。脚本完整输出是验证依据。**

## 前置条件

核心 9 模块已启动：nacos-discovery、nacos-config、gateway、provider、consumer、provider-reactive、consumer-reactive、provider-dubbo、grpc-server

---

## Step 1：执行 Trace 验证脚本

```shell
bash .qoder/skills/demo-spring-cloud/scripts/verify-trace.sh
```

**预期结果**：脚本输出五条链路的验证结果，全部显示 ✅ 通过：

| 链路                | 调用路径                                  | 协议                         | trace 传播 |
|-------------------|---------------------------------------|----------------------------|----------|
| Web→Web           | consumer → provider                   | RestTemplate / FeignClient | ✅/❌      |
| Web→gRPC          | consumer → grpc-server                | gRPC Interceptor           | ✅/❌      |
| Web→Dubbo         | consumer → provider-dubbo             | Dubbo ObservationFilter    | ✅/❌      |
| Reactive→Reactive | consumer-reactive → provider-reactive | WebClient 手动传递             | ✅/❌      |
| Reactive→Dubbo    | consumer-reactive → provider-dubbo    | Dubbo ObservationFilter    | ✅/❌      |

**必须向用户展示脚本的完整输出，然后用表格汇总五条链路验证结果**，确认每条链路的 trace ID 传播正常。

---

## Step 2（可选）：查看 Prometheus 指标

gateway、consumer-sample、business-service 三个模块集成了 `micrometer-registry-prometheus`，可访问 `/actuator/prometheus` 查看指标：

```shell
curl http://localhost:8764/actuator/prometheus   # gateway
curl http://localhost:8766/actuator/prometheus   # consumer-sample
curl http://localhost:18081/actuator/prometheus  # business-service (Seata)
```

**预期结果**：返回 Prometheus 格式的指标数据，包含 trace 相关指标。
