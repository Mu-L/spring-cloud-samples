# 🔄 Seata 分布式事务演示

前置条件：MySQL + Seata Server，请参考 [seata-sample/README](../../../../cloud-seata-sample/README.md) 中的环境准备和运行示例。

包含 7 个子模块，按依赖关系分三层启动：

| 层级 | 服务                    | 端口    | 说明                                      |
|----|-----------------------|-------|-----------------------------------------|
| 1  | account-dubbo-service | 50071 | 账户服务 Dubbo 实现（基础层）                      |
| 1  | account-service       | 18084 | 账户服务 REST 实现                            |
| 1  | storage-dubbo-service | 50072 | 库存服务 Dubbo 实现（基础层）                      |
| 1  | storage-service       | 18082 | 库存服务 REST 实现                            |
| 2  | order-dubbo-service   | 50073 | 订单服务 Dubbo 实现（依赖 account-dubbo-service） |
| 2  | order-service         | 18083 | 订单服务 REST 实现（依赖 account-service）        |
| 3  | business-service      | 18081 | 业务入口（依赖 storage + order）                |

验证分布式事务的回滚与提交，支持三种调用链路：
```shell
# RestTemplate 链路（business → storage-service, business → order-service → account-service）
curl http://localhost:18081/seata/rest

# FeignClient 链路（business → storage-service, business → order-service → account-service）
curl http://localhost:18081/seata/feign

# DubboReference 链路（business → storage-dubbo, business → order-dubbo → account-dubbo）
curl http://localhost:18081/seata/dubbo
```
> order-service 内置随机异常模拟，多次调用可观察到事务回滚（数据恢复）和提交（数据扣减）两种场景。
