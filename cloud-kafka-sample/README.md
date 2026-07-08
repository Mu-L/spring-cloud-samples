# Kafka 4.x

download kafka_2.13-4.3.1.tgz

## 单节点模式（Standalone）

生成集群ID：打开终端，进入Kafka解压目录，执行以下命令生成一个唯一的集群标识符（Cluster ID）并保存它
```shell
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
echo $KAFKA_CLUSTER_ID
```

格式化存储目录：使用上一步生成的KAFKA_CLUSTER_ID来格式化Kafka的数据存储目录。此操作只需在首次启动前执行一次
```shell
bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/server.properties
```

> 注意：请务必使用 config/server.properties 进行格式化，如果误用 config/kraft/server.properties 可能导致格式错误

启动Kafka服务：一切就绪后，执行启动命令
```shell
bin/kafka-server-start.sh config/server.properties
```

## 3节点集群模式（KRaft Cluster）

以下演示在同一台机器上启动3个节点组成集群，多机部署只需将节点分配到不同机器并调整 `advertised.listeners` 即可。

### 1. 生成集群ID
```shell
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
echo $KAFKA_CLUSTER_ID
```

### 2. 准备3份配置文件

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

### 3. 格式化3个节点的存储目录

每个节点都需要用**同一个** KAFKA_CLUSTER_ID 格式化：
```shell
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-1.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-2.properties
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server-3.properties
```

### 4. 依次启动3个节点

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

### 5. 验证集群

连接任一Broker地址即可，例如：
```shell
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic test-cluster --partitions 3 --replication-factor 3
bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic test-cluster
```

`replication-factor=3` 表示每个分区在3个节点上都有副本，任意1个节点宕机服务仍可用。

### 6. 停止集群

**本地开发（多节点在同一台机器）：**
```shell
bin/kafka-server-stop.sh
```
该命令会停止本机所有 Kafka 实例。

**生产环境（多机部署）：**

`kafka-server-stop.sh` 只停止当前机器上的 Kafka 进程，需在每台机器上分别执行。推荐滚动停止，保证服务不中断：
```shell
# 机器1
bin/kafka-server-stop.sh
# 等待完全停止后，继续下一台
# 机器2
bin/kafka-server-stop.sh
# 机器3
bin/kafka-server-stop.sh
```

> 停止后请等待几秒让进程完全关闭，避免元数据损坏。

## 常见问题

### 本地开发元数据损坏

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