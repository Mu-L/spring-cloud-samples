# 🔍 Trace 链路追踪

项目内置 trace 传播验证脚本，覆盖五条跨服务链路，验证 Spring Boot Observation 与各框架的 trace context 自动/手动传播：

| 链路                  | 路径                                    | trace 传播                        |
|---------------------|---------------------------------------|---------------------------------|
| Web → Web           | consumer → provider                   | RestTemplate / FeignClient 自动传播 |
| Web → gRPC          | consumer → grpc-server                | gRPC Interceptor 自动传播           |
| Web → Dubbo         | consumer → provider-dubbo             | Dubbo ObservationFilter 自动传播    |
| Reactive → Reactive | consumer-reactive → provider-reactive | WebClient 手动传递 traceparent      |
| Reactive → Dubbo    | consumer-reactive → provider-dubbo    | Dubbo ObservationFilter 自动传播    |

```shell
bash .qoder/skills/demo-spring-cloud/scripts/verify-trace.sh
```

此外，gateway、consumer-sample、business-service 三个模块集成了 `micrometer-registry-prometheus`，可直接访问 `/actuator/prometheus` 查看 Prometheus 格式的指标数据：
```shell
curl http://localhost:8764/actuator/prometheus   # gateway
curl http://localhost:8766/actuator/prometheus   # consumer-sample
curl http://localhost:18081/actuator/prometheus  # business-service (Seata)
```
