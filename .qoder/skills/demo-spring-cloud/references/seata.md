# 🔄 Seata 分布式事务演示

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

### 前置条件

#### 1. MySQL

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

#### 2. 初始化数据库

在 MySQL 中创建 `seata` 数据库，执行 `cloud-seata-sample/all.sql`：

```shell
mysql -u root -proot1234 -e "CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -proot1234 seata < cloud-seata-sample/all.sql
```

- **Seata 基础表**：`undo_log`、`global_table`、`branch_table`、`lock_table`、`distributed_lock`
- **业务表**：`storage_tbl`、`order_tbl`、`account_tbl`

如需修改数据库连接信息，编辑各服务 `resources/application.yml`。

#### 3. 启动 Nacos

确保 Nacos 已在 `127.0.0.1:8848` 上运行。

#### 4. 配置 Nacos（Seata 配置中心）

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

#### 5. 启动 Seata Server

**检查 Seata Server**（端口 8091）：
```shell
nc -z 127.0.0.1 8091 && echo "✓ Seata Server 已运行" || echo "✗ Seata Server 未运行"
```

目前下载的二进制包里面依赖的 `nacos-client` 是 1.4.6 版本，版本太低，而本地的是 3.2.2 版本，需使用源码构建来启动：

```shell
# 本克隆版本已经修改了 application.yml 配置，直接克隆下来修改其中的 nacos 密码后启动就行
SEATA_SRC="$HOME/github/seata"
[ ! -d "$SEATA_SRC" ] && mkdir -p "$HOME/github" && git clone https://github.com/javahongxi/seata.git "$SEATA_SRC"
cd "$SEATA_SRC" && ./mvnw clean install -DskipTests -q
nohup ./mvnw -pl server spring-boot:run > /tmp/seata-server.log 2>&1 &
for i in $(seq 1 30); do
  nc -z 127.0.0.1 8091 2>/dev/null && echo "✓ Seata Server 已启动" && break
  sleep 1
done
```

### 运行示例

按层级顺序依次启动：

1. **层级 1**：`AccountDubboApplication`、`AccountApplication`、`StorageDubboApplication`、`StorageApplication`
2. **层级 2**：`OrderDubboApplication`、`OrderApplication`
3. **层级 3**：`BusinessApplication`

### 验证分布式事务

支持三种调用链路：

```shell
# RestTemplate 链路（business → storage-service, business → order-service → account-service）
curl http://localhost:18081/seata/rest

# FeignClient 链路（business → storage-service, business → order-service → account-service）
curl http://localhost:18081/seata/feign

# DubboReference 链路（business → storage-dubbo, business → order-dubbo → account-dubbo）
curl http://localhost:18081/seata/dubbo
```

返回结果说明：
- **SUCCESS**：调用成功
- **500 异常**：business-service 中 mock 的随机异常（用于验证事务回滚）

#### Xid 传递

查看各服务控制台日志，确认同一次请求中所有服务输出的 Xid 一致：

```
Storage Service Begin ... xid: 192.168.x.x:8091:xxxx
Order Service Begin ... xid: 192.168.x.x:8091:xxxx
Account Service Begin ... xid: 192.168.x.x:8091:xxxx
```

#### 数据一致性

OrderService 和 AccountService 中通过 `Random.nextBoolean()` 随机抛出异常来模拟失败场景。如果分布式事务正常工作，以下等式应始终成立：

- **用户余额**：`10000 = 当前余额 + 2(单价) × 订单数 × 2(每单数量)`
- **库存数量**：`100 = 当前库存 + 订单数 × 2(每单数量)`

```sql
SELECT * FROM account_tbl;
SELECT * FROM storage_tbl;
SELECT * FROM order_tbl;
```
