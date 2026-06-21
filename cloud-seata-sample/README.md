# Seata 分布式事务示例

## 项目说明

本模块演示如何使用 Spring Cloud Alibaba Seata 完成分布式事务接入，包含以下四个微服务：

| 服务 | 端口 | 说明 |
|------|------|------|
| business-service | 18081 | 业务服务（入口），通过 RestTemplate / FeignClient 调用下游服务 |
| storage-service | 18082 | 库存服务，负责扣减商品库存 |
| order-service | 18083 | 订单服务，负责创建订单并调用账户服务 |
| account-service | 18084 | 账户服务，负责扣减用户余额 |

## 准备工作

### 1. 初始化数据库

在 MySQL 中创建一个名为 `seata` 的数据库，然后执行 `all.sql` 脚本完成以下表的创建：

- **Seata 基础表**：`undo_log`、`global_table`、`branch_table`、`lock_table`、`distributed_lock`
- **业务表**：`storage_tbl`、`order_tbl`、`account_tbl`

如需修改数据库连接信息，请编辑各服务 `resources/application.yml` 中的以下配置：

```yaml
base:
  config:
    mdb:
      hostname: 127.0.0.1
      dbname: seata
      port: 3306
      username: 'root'
      password: 'root'
```

### 2. 启动 Nacos

确保 Nacos 已在 `127.0.0.1:8848` 上运行。

### 3. 配置 Nacos（Seata 配置中心）

在 Nacos 中创建配置：

- **Data ID**：`seata.properties`
- **Group**：`SEATA_GROUP`
- **配置格式**：Properties

配置内容：

```properties
service.vgroupMapping.default_tx_group=default
service.vgroupMapping.order-service-tx-group=default
service.vgroupMapping.account-service-tx-group=default
service.vgroupMapping.business-service-tx-group=default
service.vgroupMapping.storage-service-tx-group=default
```

### 4. 启动 Seata Server
目前下载的二进制包里面依赖的`nacos-client`是1.4.6版本，版本太低，而本地的是3.2.1版本，只好使用源码构建来启动
```text
git clone https://github.com/apache/incubator-seata
修改 server 模块下的 application.yml，如下方yaml配置所示
构建整个项目，启动 server 模块 ServerApplication
```

```yaml
seata:
  config:
    type: nacos 
    nacos:
      server-addr: 127.0.0.1:8848
      username: 'nacos'
      password: '7fDJZBbiLzO2'
      group: SEATA_GROUP
      namespace: public
      data-id: seata.properties
  registry:
    type: nacos
    nacos:
      application: seata-server
      group: SEATA_GROUP
      namespace: public
      cluster: default
      server-addr: 127.0.0.1:8848
      username: 'nacos'
      password: '7fDJZBbiLzO2'
```

```shell
sh seata-server.sh
```

## 运行示例

依次启动以下四个应用的主类：

1. `StorageApplication`（storage-service）
2. `AccountApplication`（account-service）
3. `OrderApplication`（order-service）
4. `BusinessApplication`（business-service）

启动完成后，通过 GET 请求访问以下接口：

```
# 通过 FeignClient 调用
http://127.0.0.1:18081/seata/feign

# 通过 RestTemplate 调用
http://127.0.0.1:18081/seata/rest
```

返回结果说明：
- **SUCCESS**：调用成功
- **500 异常**：business-service 中 mock 的随机异常（用于验证事务回滚）

## 验证分布式事务

### Xid 传递

查看各服务控制台日志，确认同一次请求中所有服务输出的 Xid 一致：

```
Storage Service Begin ... xid: 192.168.x.x:8091:xxxx
Order Service Begin ... xid: 192.168.x.x:8091:xxxx
Account Service ... xid: 192.168.x.x:8091:xxxx
```

### 数据一致性

OrderService 和 AccountService 中通过 `Random.nextBoolean()` 随机抛出异常来模拟失败场景。如果分布式事务正常工作，以下等式应始终成立：

- **用户余额**：`1000 = 当前余额 + 2(单价) × 订单数 × 2(每单数量)`
- **库存数量**：`100 = 当前库存 + 订单数 × 2(每单数量)`

```sql
SELECT * FROM account_tbl;
SELECT * FROM storage_tbl;
SELECT * FROM order_tbl;
```
