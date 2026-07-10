# 📨 Stream 消息驱动演示

演示 Spring Cloud Stream 的六大核心场景：

| 场景     | 函数类型     | 消息流                                       | 说明                          |
|--------|----------|-------------------------------------------|-----------------------------|
| 基础消费   | Consumer | StreamBridge → topic → input              | 启动时自动发送 "Hello" 并消费         |
| 定时消息源  | Supplier | output2 → topic2 → input2                 | 每隔1秒自动发送 "你好"               |
| 消息处理管道 | Function | REST → transform → [toUpperCase] → topic2 | 消息转换后输出                     |
| 延迟消息   | Consumer | StreamBridge → delay-topic → delay        | 通过 DELAY header 指定延迟级别后延迟投递 |
| 顺序消息   | Consumer | StreamBridge → fifo-topic → fifo          | 相同 orderKey 保证顺序消费          |
| 事务消息   | Consumer | StreamBridge → tx-topic → tx              | 两阶段提交，随机模拟本地事务成功/失败         |

前置条件：本地运行 RocketMQ

**检查 RocketMQ**：
```shell
nc -z 127.0.0.1 9876 && echo "✓ RocketMQ NameServer 已运行" || echo "✗ RocketMQ 未运行"
```

若未安装：
```shell
curl -O https://dist.apache.org/repos/dist/release/rocketmq/5.5.0/rocketmq-all-5.5.0-bin-release.zip
unzip rocketmq-all-5.5.0-bin-release.zip -d $HOME
```

启动 NameServer + Broker 并验证：
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

创建 Topic 和 Consumer Group：
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

启动 `stream`，观察日志（基础消费 + 定时消息源自动触发）

场景1、2 启动后自动触发，观察日志即可。场景2 验证后通过 Actuator 端点停止定时消息源，避免后续场景日志刷屏：
```shell
curl -s -X POST "http://localhost:8767/actuator/bindings/output2-out-0" -H "Content-Type: application/json" -d '{"state":"STOPPED"}'
```

继续通过 REST API 交互式验证各场景：
```shell
# 场景3: 消息处理管道 - 发送消息到 transform 函数（观察大写转换）
curl -X POST "http://localhost:8767/stream/send?message=hello+spring+cloud"
# 日志观察: 消息转换: hello spring cloud -> [PROCESSED] HELLO SPRING CLOUD

# 场景4: 延迟消息 - 发送延迟消息（delayLevel=2 即 5秒后投递）
curl -X POST "http://localhost:8767/stream/delay?message=hello+delay&delayLevel=2"
# 日志观察: [延迟消息] 收到: hello delay (时间: ...) — 注意接收时间与发送时间差约5秒

# 场景5: 顺序消息 - 发送带相同 orderKey 的消息（保证顺序消费）
curl -X POST "http://localhost:8767/stream/fifo?message=order-1&orderKey=order-A"
curl -X POST "http://localhost:8767/stream/fifo?message=order-2&orderKey=order-A"
curl -X POST "http://localhost:8767/stream/fifo?message=order-3&orderKey=order-A"
# 日志观察: [顺序消息] 收到消息按发送顺序依次被消费

# 场景6: 事务消息 - 发送事务消息（两阶段提交，随机决定提交或回滚）
curl -X POST "http://localhost:8767/stream/tx?message=hello+tx"
# 日志观察: [事务消息] 执行本地事务 → [事务消息] 本地事务提交 (随机) 或 本地事务回滚 (随机)
# 多次调用可观察到 commit 和 rollback 两种场景
```

查看消费组的消费进度：
```shell
bin/mqadmin consumerProgress -n localhost:9876 -g stream-demo-consumer-group2
```
