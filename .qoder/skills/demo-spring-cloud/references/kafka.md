# 📨 Kafka 4.x 消息收发演示

基于 **Apache Kafka 4.x**（KRaft 模式）演示传统 Consumer Group、Share Groups 特性（允许多消费者从同一分区并行消费，支持逐条消息确认）以及事务消息。

## Kafka 集群部署

下载 `kafka_2.13-4.3.1.tgz` 并解压，进入 Kafka 解压目录。

### 单节点模式（Standalone）

生成集群ID：
```shell
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
echo $KAFKA_CLUSTER_ID
```

格式化存储目录（首次启动前执行一次）：
```shell
bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/server.properties
```

> 注意：请务必使用 `config/server.properties` 进行格式化，误用 `config/kraft/server.properties` 可能导致格式错误

启动 Kafka 服务：
```shell
bin/kafka-server-start.sh config/server.properties
```

### 3节点集群模式（KRaft Cluster）

以下演示在同一台机器上启动3个节点组成集群，多机部署只需将节点分配到不同机器并调整 `advertised.listeners` 即可。

**1. 生成集群ID**
```shell
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
echo $KAFKA_CLUSTER_ID
```

**2. 准备3份配置文件**

基于 `config/server.properties` 复制3份，分别修改关键配置：

**config/server-1.properties**
```properties
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@localhost:9093,2@localhost:9095,3@localhost:9097
controller.listener.names=CONTROLLER
listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093
advertised.listeners=PLAINTEXT://localhost:9092
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
log.dirs=/tmp/kraft-logs-1
```

**config/server-2.properties**
```properties
process.roles=broker,controller
node.id=2
controller.quorum.voters=1@localhost:9093,2@localhost:9095,3@localhost:9097
controller.listener.names=CONTROLLER
listeners=PLAINTEXT://localhost:9094,CONTROLLER://localhost:9095
advertised.listeners=PLAINTEXT://localhost:9094
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
log.dirs=/tmp/kraft-logs-2
```

**config/server-3.properties**
```properties
process.roles=broker,controller
node.id=3
controller.quorum.voters=1@localhost:9093,2@localhost:9095,3@localhost:9097
controller.listener.names=CONTROLLER
listeners=PLAINTEXT://localhost:9096,CONTROLLER://localhost:9097
advertised.listeners=PLAINTEXT://localhost:9096
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
log.dirs=/tmp/kraft-logs-3
```

> 每个节点的 `node.id` 必须唯一，`controller.quorum.voters` 必须一致，端口和 `log.dirs` 不能冲突。

**3. 格式化3个节点的存储目录**

每个节点都需要用**同一个** KAFKA_CLUSTER_ID 格式化：
```shell
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-1.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-2.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-3.properties
```

**4. 依次启动3个节点**

每个节点在**独立的终端窗口**中启动：
```shell
# 终端1
bin/kafka-server-start.sh config/server-1.properties

# 终端2
bin/kafka-server-start.sh config/server-2.properties

# 终端3
bin/kafka-server-start.sh config/server-3.properties
```

> 启动第1个节点时会刷屏 WARN 日志（如 `Connection to node 2 could not be established`），这是正常的，因为其他节点尚未启动。继续启动第2、3个节点后，集群会自动组网，日志恢复正常。

**5. 验证集群**

连接任一 Broker 地址即可：
```shell
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic test-cluster --partitions 3 --replication-factor 3
bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic test-cluster
```

`replication-factor=3` 表示每个分区在3个节点上都有副本，任意1个节点宕机服务仍可用。

**6. 停止集群**

本地开发（多节点在同一台机器）：
```shell
bin/kafka-server-stop.sh
```
该命令会停止本机所有 Kafka 实例。

生产环境（多机部署）需在每台机器上分别执行 `bin/kafka-server-stop.sh`，推荐滚动停止，保证服务不中断。

> 停止后请等待几秒让进程完全关闭，避免元数据损坏。

### 常见问题：元数据损坏

强制终止 Kafka（如直接关闭终端窗口）可能导致 `meta.properties` 文件丢失或损坏，再次启动时报错：
```
java.lang.RuntimeException: No readable meta.properties files found.
```

**解决方法：清理日志目录并重新格式化**

单节点模式：
```shell
rm -rf /tmp/kraft-combined-logs
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/server.properties
```

3节点集群模式：
```shell
rm -rf /tmp/kraft-logs-1 /tmp/kraft-logs-2 /tmp/kraft-logs-3
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-1.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-2.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-3.properties
```

> 本地开发环境无需保留数据，重新格式化即可。生产环境应通过 `kafka-server-stop.sh` 优雅停止，避免此问题。

---

## 创建 Topic

Kafka 集群启动后，创建演示所需的 Topic：

```shell
KAFKA_HOME=$(find "$HOME" -maxdepth 1 -type d -name 'kafka_*' | sort -V | tail -1)
$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic share-demo-topic --partitions 3 --replication-factor 3
$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic share-demo-topic-explicit --partitions 3 --replication-factor 3
$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic tx-demo-topic --partitions 3 --replication-factor 3
```

## 启动模块

```shell
./mvnw -pl cloud-kafka-sample spring-boot:run
```

启动后 `ApplicationRunner` 自动发送传统 Consumer Group 消息，日志中可观察到：
```
Sent sample message [SampleMessage{id=1, message='test'}] to topic [testTopic]
Received sample message [SampleMessage{id=1, message='test'}]
```

## Share Groups 验证

通过 REST 接口触发 Share Group 消息发送：
```shell
# 发送 Share Group 隐式确认消息（默认10条）
curl -X POST "http://localhost:8768/kafka/share/implicit?count=10"

# 发送 Share Group 显式确认消息（演示重试，id=5,10,15会重投递）
curl -X POST "http://localhost:8768/kafka/share/explicit?count=15"
```

查看日志确认 Share Group 消息收发：
```shell
grep -aE "\[Share-" logs/kafka-sample.log | head -50
```

## 事务消息验证

事务消息使用独立的 `KafkaTemplate`（配置 `transactional.id`），消费者采用 `read_committed` 隔离级别，只读取已提交事务的消息。

```shell
# 事务提交 - 消费者可读到消息
curl -X POST "http://localhost:8768/kafka/tx/commit?count=5"

# 事务回滚 - 消费者读不到消息
curl -X POST "http://localhost:8768/kafka/tx/rollback?count=5"

# 查看事务消息日志
grep -aE "\[TX" logs/kafka-sample.log | tail -20
```

> **核心特性**：
> - **并行消费**：同一分区的消息可被多个消费者同时处理（传统模式仅允许单消费者）
> - **逐条确认**：支持 ACK/NACK 机制，精确控制每条消息的确认、重试或拒绝
> - **隐式确认**：方法正常返回自动 ACCEPT，抛出异常自动 REJECT
> - **显式确认**：手动调用 `acknowledgment.acknowledge()/release()/reject()` 精细控制
> - **重试演示**：id 为 5 的倍数的消息会触发 release，最多重投递 5 次（Kafka 默认 `group.share.delivery.count.limit=5`）后停止
> - **事务消息**：`executeInTransaction` 原子发送，事务提交前消费者不可见（read_committed 隔离级别）
