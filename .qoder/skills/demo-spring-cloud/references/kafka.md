# 📨 Kafka 4.x 消息收发演示

基于 **Apache Kafka 4.x**（KRaft 模式）演示传统 Consumer Group、Share Groups 特性（允许多消费者从同一分区并行消费，支持逐条消息确认）以及事务消息。

前置条件：Kafka 4.x 3节点集群已启动，详见 [cloud-kafka-sample/README.md](../../../../cloud-kafka-sample/README.md)

## 创建 Topic
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
