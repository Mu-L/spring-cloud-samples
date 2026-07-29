# 🔄 Seata 分布式事务演示

> 🔴 **共 9 个步骤，必须逐一执行，不可跳过。每步执行后确认结果是否符合预期。**

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

---

## Step 1：检查并初始化 MySQL

**检查 MySQL**：
```shell
mysql -u root -proot1234 -e "SELECT 1"
```

若 MySQL 未安装或密码不对：
```shell
brew install mysql
mysql.server start
mysqladmin -u root password 'root1234'
```

> 项目统一使用 root/root1234，若已有 MySQL 且密码不同，请重置密码或自行修改各模块 application.yml 中的数据库配置。

**初始化数据库**：
```shell
mysql -u root -proot1234 -e "CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -proot1234 seata < cloud-seata-sample/all.sql
```

**预期结果**：数据库 `seata` 创建成功，包含 `undo_log`、`global_table`、`branch_table`、`lock_table`、`distributed_lock`、`storage_tbl`、`order_tbl`、`account_tbl` 等表。

---

## Step 2：配置 Nacos（Seata 配置中心）

确保 Nacos 已在 `127.0.0.1:8848` 上运行。

在 Nacos 中创建配置：
- **Data ID**：`seata.properties`
- **Group**：`SEATA_GROUP`
- **配置格式**：Properties

```properties
service.vgroupMapping.default_tx_group=default
service.vgroupMapping.order-service-tx-group=default
service.vgroupMapping.account-service-tx-group=default
service.vgroupMapping.business-service-tx-group=default
service.vgroupMapping.storage-service-tx-group=default
service.vgroupMapping.order-dubbo-service-tx-group=default
service.vgroupMapping.account-dubbo-service-tx-group=default
service.vgroupMapping.business-dubbo-service-tx-group=default
service.vgroupMapping.storage-dubbo-service-tx-group=default
```

---

## Step 3：启动 Seata Server

**检查 Seata Server**（端口 8091）：
```shell
nc -z 127.0.0.1 8091 && echo "✓ Seata Server 已运行" || echo "✗ Seata Server 未运行"
```

目前下载的二进制包里面依赖的 `nacos-client` 是 1.4.6 版本，版本太低，而本地的是 3.2.2 版本，需使用源码构建来启动：

```shell
SEATA_SRC="$HOME/github/seata"
if [ ! -d "$SEATA_SRC" ]; then
  mkdir -p "$HOME/github"
  curl -L -o /tmp/seata-2.x.zip https://github.com/javahongxi/seata/archive/refs/heads/2.x.zip
  unzip -o /tmp/seata-2.x.zip -d "$HOME/github"
  mv "$HOME/github/seata-2.x" "$SEATA_SRC"
  rm -f /tmp/seata-2.x.zip
fi
cd "$SEATA_SRC" && ./mvnw clean install -DskipTests -q
nohup ./mvnw -pl server spring-boot:run > /tmp/seata-server.log 2>&1 &
for i in $(seq 1 30); do
  nc -z 127.0.0.1 8091 2>/dev/null && echo "✓ Seata Server 已启动" && break
  sleep 1
done
```

**预期结果**：Seata Server 在端口 8091 上运行。

---

## Step 4：按三层依赖启动 7 个微服务

按层级顺序依次启动：

1. **层级 1**：`AccountDubboApplication`、`AccountApplication`、`StorageDubboApplication`、`StorageApplication`
2. **层级 2**：`OrderDubboApplication`、`OrderApplication`
3. **层级 3**：`BusinessApplication`

**预期结果**：所有 7 个服务启动成功，无报错。

---

## Step 5：验证 RestTemplate 链路

```shell
curl http://localhost:18081/seata/rest
```

**预期结果**：返回 `SUCCESS` 或 `500 异常`（business-service 中 mock 的随机异常）。多次调用可观察到两种结果。

---

## Step 6：验证 Feign 链路

```shell
curl http://localhost:18081/seata/feign
```

**预期结果**：返回 `SUCCESS` 或 `500 异常`（随机）。

---

## Step 7：验证 Dubbo 链路

```shell
curl http://localhost:18081/seata/dubbo
```

**预期结果**：返回 `SUCCESS` 或 `500 异常`（随机）。

---

## Step 8：检查 Xid 传递一致性

查看各服务控制台日志，确认同一次请求中所有服务输出的 Xid 一致：

```
Storage Service Begin ... xid: 192.168.x.x:8091:xxxx
Order Service Begin ... xid: 192.168.x.x:8091:xxxx
Account Service Begin ... xid: 192.168.x.x:8091:xxxx
```

**预期结果**：同一次请求中，Storage、Order、Account 三个服务输出的 Xid 完全一致，证明分布式事务上下文正确传播。

---

## Step 9：验证数据一致性

OrderService 和 AccountService 中通过 `Random.nextBoolean()` 随机抛出异常来模拟失败场景。如果分布式事务正常工作，以下等式应始终成立：

- **用户余额**：`10000 = 当前余额 + 2(单价) × 订单数 × 2(每单数量)`
- **库存数量**：`100 = 当前库存 + 订单数 × 2(每单数量)`

```sql
SELECT * FROM account_tbl;
SELECT * FROM storage_tbl;
SELECT * FROM order_tbl;
```

**预期结果**：数据一致性等式成立，事务回滚时数据正确恢复。
