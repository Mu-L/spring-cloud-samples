# 📨 Stream 消息驱动演示

> 🔴 **共 8 个步骤，必须逐一执行，不可跳过。每步执行后确认结果是否符合预期。**

演示 Spring Cloud Stream 的六大核心场景：

| 场景     | 函数类型     | 消息流                                       | 说明                          |
|--------|----------|-------------------------------------------|-----------------------------|
| 基础消费   | Consumer | StreamBridge → topic → input              | 启动时自动发送 "Hello" 并消费         |
| 定时消息源  | Supplier | output2 → topic2 → input2                 | 每隔1秒自动发送 "你好"               |
| 消息处理管道 | Function | REST → transform → [toUpperCase] → topic2 | 消息转换后输出                     |
| 延迟消息   | Consumer | StreamBridge → delay-topic → delay        | 通过 DELAY header 指定延迟级别后延迟投递 |
| 顺序消息   | Consumer | StreamBridge → fifo-topic → fifo          | 相同 orderKey 保证顺序消费          |
| 事务消息   | Consumer | StreamBridge → tx-topic → tx              | 两阶段提交，随机模拟本地事务成功/失败         |

## 前置条件

本地运行 RocketMQ。

**检查 RocketMQ**：
```shell
nc -z 127.0.0.1 9876 && echo "✓ RocketMQ NameServer 已运行" || echo "✗ RocketMQ 未运行"
```

若未安装：
```shell
curl -O https://dist.apache.org/repos/dist/release/rocketmq/5.5.0/rocketmq-all-5.5.0-bin-release.zip
unzip rocketmq-all-5.5.0-bin-release.zip -d $HOME
```

---

## Step 1：启动 RocketMQ NameServer + Broker

```shell
ROCKETMQ_HOME=$(find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' | sort -V | tail -1)
cd "$ROCKETMQ_HOME"
nohup bin/mqnamesrv > namesrv.log 2>&1 &
sleep 5
nohup bin/mqbroker -n localhost:9876 > broker.log 2>&1 &
sleep 10
nc -z 127.0.0.1 9876 && echo "✓ NameServer 已启动" || echo "✗ NameServer 启动失败"
nc -z 127.0.0.1 10911 && echo "✓ Broker 已启动" || echo "✗ Broker 启动失败"
```

**预期结果**：NameServer 和 Broker 均显示 ✓ 已启动。

---

## Step 2：创建 Topic 和 Consumer Group

```shell
# 基础消息
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-demo-consumer-group
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic2 -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-demo-consumer-group2
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-transform-topic -a +message.type=NORMAL
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-transform-group
# 延迟消息（DELAY 类型）
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-delay-topic -a +message.type=DELAY
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-delay-group
# 顺序消息（FIFO 类型）
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-fifo-topic -a +message.type=FIFO
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-fifo-group
# 事务消息（TRANSACTION 类型）
bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-tx-topic -a +message.type=TRANSACTION
bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-tx-group
```

**预期结果**：每个 Topic 和 Consumer Group 创建成功。

---

## Step 3：启动 stream 模块，观察自动触发的场景

启动 `stream` 模块：
```shell
./mvnw -pl cloud-stream-sample spring-boot:run
```

**预期结果**：日志中自动出现：
- 场景 1（基础消费）：收到 "Hello" 消息
- 场景 2（定时消息源）：每隔 1 秒收到 "你好" 消息

---

## Step 4：停止定时消息源（避免后续场景日志刷屏）

```shell
curl -s -X POST "http://localhost:8767/actuator/bindings/output2-out-0" -H "Content-Type: application/json" -d '{"state":"STOPPED"}'
```

**预期结果**：定时消息源停止，日志中不再出现每秒 "你好" 消息。

---

## Step 5：验证消息处理管道（场景 3）

```shell
curl -X POST "http://localhost:8767/stream/send?message=hello+spring+cloud"
```

**预期结果**：日志观察消息转换：`hello spring cloud -> [PROCESSED] HELLO SPRING CLOUD`

---

## Step 6：验证延迟消息（场景 4）

```shell
curl -X POST "http://localhost:8767/stream/delay?message=hello+delay&delayLevel=2"
```

**预期结果**：日志中 `[延迟消息] 收到: hello delay`，注意接收时间与发送时间差约 5 秒（delayLevel=2 对应 5s）。

---

## Step 7：验证顺序消息（场景 5）

```shell
curl -X POST "http://localhost:8767/stream/fifo?message=order-1&orderKey=order-A"
curl -X POST "http://localhost:8767/stream/fifo?message=order-2&orderKey=order-A"
curl -X POST "http://localhost:8767/stream/fifo?message=order-3&orderKey=order-A"
```

**预期结果**：日志中 `[顺序消息]` 收到消息按发送顺序依次被消费（order-1 → order-2 → order-3）。

---

## Step 8：验证事务消息（场景 6）

```shell
curl -X POST "http://localhost:8767/stream/tx?message=hello+tx"
```

多次调用（至少 3 次），观察不同结果：

**预期结果**：
- 部分调用日志显示 `[事务消息] 本地事务提交`（commit）
- 部分调用日志显示 `[事务消息] 本地事务回滚`（rollback）
- 随机决定，多次调用可观察到两种场景

---

## 查看消费进度（可选）

```shell
bin/mqadmin consumerProgress -n localhost:9876 -g stream-demo-consumer-group2
```

> **核心特性总结**：
> - **并行消费**：同一分区的消息可被多个消费者同时处理（传统模式仅允许单消费者）
> - **逐条确认**：支持 ACK/NACK 机制，精确控制每条消息的确认、重试或拒绝
> - **隐式确认**：方法正常返回自动 ACCEPT，抛出异常自动 REJECT
> - **显式确认**：手动调用 `acknowledgment.acknowledge()/release()/reject()` 精细控制
> - **重试演示**：id 为 5 的倍数的消息会触发 release，最多重投递 5 次（Kafka 默认 `group.share.delivery.count.limit=5`）后停止
> - **事务消息**：`executeInTransaction` 原子发送，事务提交前消费者不可见（read_committed 隔离级别）
