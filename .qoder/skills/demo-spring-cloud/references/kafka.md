# 📨 Kafka 4.x 消息收发演示

> 🔴 **演示分为两阶段：集群部署（一次性）+ 消息收发验证（每次演示）。所有步骤必须逐一执行，不可跳过。**

基于 **Apache Kafka 4.x**（KRaft 模式）演示传统 Consumer Group、Share Groups 特性（允许多消费者从同一分区并行消费，支持逐条消息确认）以及事务消息。

---

# 阶段一：Kafka 集群部署（首次或集群未运行时执行）

下载 `kafka_2.13-4.3.1.tgz` 并解压，进入 Kafka 解压目录。

> kafka-sample 模块配置了 3 节点集群地址 `localhost:9092,localhost:9094,localhost:9096`，必须部署 3 节点 KRaft 集群。

## Step 1：3 节点集群模式部署

**1.1 生成集群 ID**
```shell
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
```

**1.2 准备 3 份配置文件**

基于 `config/server.properties` 复制 3 份，关键配置如下：

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

**1.3 格式化 3 个节点**（使用同一个 KAFKA_CLUSTER_ID）
```shell
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-1.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-2.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-3.properties
```

**1.4 依次启动 3 个节点**（每个节点在独立终端窗口中）
```shell
# 终端 1
bin/kafka-server-start.sh config/server-1.properties
# 终端 2
bin/kafka-server-start.sh config/server-2.properties
# 终端 3
bin/kafka-server-start.sh config/server-3.properties
```

> 启动第 1 个节点时会刷屏 WARN 日志，这是正常的，继续启动第 2、3 个节点后集群自动组网。

**1.5 验证集群**
```shell
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic test-cluster --partitions 3 --replication-factor 3
bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic test-cluster
```

**预期结果**：`replication-factor=3`，每个分区在 3 个节点上都有副本。

**1.6 停止集群**
```shell
bin/kafka-server-stop.sh
```

> 停止后等待几秒让进程完全关闭，避免元数据损坏。

### 常见问题：元数据损坏

强制终止 Kafka 可能导致 `meta.properties` 损坏，再次启动报错：
```
java.lang.RuntimeException: No readable meta.properties files found.
```

**解决方法：清理日志目录并重新格式化**
```shell
rm -rf /tmp/kraft-logs-1 /tmp/kraft-logs-2 /tmp/kraft-logs-3
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-1.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-2.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-3.properties
```

---

# 阶段二：消息收发验证（每次演示必做）

> 🔴 **以下 Step 2~6 必须逐一执行，不可跳过。**

## Step 2：创建演示 Topic

```shell
KAFKA_HOME=$(find "$HOME" -maxdepth 1 -type d -name 'kafka_*' | sort -V | tail -1)
$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic share-demo-topic --partitions 3 --replication-factor 3
$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic share-demo-topic-explicit --partitions 3 --replication-factor 3
$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic tx-demo-topic --partitions 3 --replication-factor 3
```

**预期结果**：3 个 Topic 创建成功。

---

## Step 3：启动 Kafka 模块并观察自动消息

```shell
./mvnw -pl cloud-kafka-sample spring-boot:run
```

**预期结果**：日志中自动出现：
```
Sent sample message [SampleMessage{id=1, message='test'}] to topic [testTopic]
Received sample message [SampleMessage{id=1, message='test'}]
```

---

## Step 4：Share Groups 隐式确认

```shell
curl -X POST "http://localhost:8768/kafka/share/implicit?count=10"
```

**预期结果**：发送 10 条消息，日志中可观察到 Share Group 消费记录。

---

## Step 5：Share Groups 显式确认（含重试演示）

```shell
curl -X POST "http://localhost:8768/kafka/share/explicit?count=15"
```

**预期结果**：发送 15 条消息，id=5,10,15 会触发重投递（release），观察日志中的重试行为。

查看 Share Group 日志：
```shell
grep -aE "\[Share-" logs/kafka-sample.log | head -50
```

---

## Step 6：事务消息验证

**事务提交** - 消费者可读到消息：
```shell
curl -X POST "http://localhost:8768/kafka/tx/commit?count=5"
```

**事务回滚** - 消费者读不到消息：
```shell
curl -X POST "http://localhost:8768/kafka/tx/rollback?count=5"
```

查看事务消息日志：
```shell
grep -aE "\[TX" logs/kafka-sample.log | tail -20
```

**预期结果**：
- commit 场景：消费者成功收到消息
- rollback 场景：消费者未收到消息（事务回滚，消息被丢弃）

---

> **核心特性总结**：
> - **并行消费**：同一分区的消息可被多个消费者同时处理（传统模式仅允许单消费者）
> - **逐条确认**：支持 ACK/NACK 机制，精确控制每条消息的确认、重试或拒绝
> - **隐式确认**：方法正常返回自动 ACCEPT，抛出异常自动 REJECT
> - **显式确认**：手动调用 `acknowledgment.acknowledge()/release()/reject()` 精细控制
> - **重试演示**：id 为 5 的倍数的消息会触发 release，最多重投递 5 次（Kafka 默认 `group.share.delivery.count.limit=5`）后停止
> - **事务消息**：`executeInTransaction` 原子发送，事务提交前消费者不可见（read_committed 隔离级别）
