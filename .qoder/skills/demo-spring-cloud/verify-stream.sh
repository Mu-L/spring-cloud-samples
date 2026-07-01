#!/bin/bash
# Stream 模块验证脚本
# 用法: bash .qoder/skills/demo-spring-cloud/verify-stream.sh
cd "$(dirname "$0")/../../.."
PROJECT_DIR=$(pwd)

echo "=========================================="
echo "  Stream 消息收发验证 - 完整流程"
echo "=========================================="

ROCKETMQ_HOME=$(find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' | sort -V | tail -1)

# ========== Step 0: 清理旧进程 ==========
echo ""
echo ">>> Step 0: 清理旧进程..."
pkill -f "cloud-stream-sample" 2>/dev/null || true
sleep 1
rm -rf logs
mkdir -p logs
echo "✓ 清理完成"

# ========== Step 1: 检查 Nacos ==========
echo ""
echo ">>> Step 1: 检查 Nacos..."
if curl -s -o /dev/null -w "" "http://127.0.0.1:8848/nacos/actuator/health" 2>/dev/null; then
  echo "✓ Nacos 已运行"
else
  echo "✗ Nacos 未运行，请先启动 Nacos"
  exit 1
fi

# ========== Step 2: 启动 RocketMQ ==========
echo ""
echo ">>> Step 2: 检查/启动 RocketMQ..."

if nc -z 127.0.0.1 9876 2>/dev/null; then
  echo "✓ RocketMQ NameServer 已在运行"
else
  if [ -z "$ROCKETMQ_HOME" ] || [ ! -d "$ROCKETMQ_HOME" ]; then
    echo "✗ RocketMQ 未安装，请在 $HOME 下安装 rocketmq"
    exit 1
  fi

  echo "启动 NameServer..."
  cd "$ROCKETMQ_HOME"
  nohup bin/mqnamesrv > namesrv.log 2>&1 &
  sleep 5

  if nc -z 127.0.0.1 9876 2>/dev/null; then
    echo "✓ NameServer 已启动 (端口 9876)"
  else
    echo "✗ NameServer 启动失败"
    tail -20 namesrv.log
    exit 1
  fi

  echo "启动 Broker..."
  nohup bin/mqbroker -n localhost:9876 > broker.log 2>&1 &
  sleep 10

  if nc -z 127.0.0.1 10911 2>/dev/null; then
    echo "✓ Broker 已启动 (端口 10911)"
  else
    echo "✗ Broker 启动失败"
    tail -20 broker.log
    exit 1
  fi
fi

# ========== Step 3: 检查/创建 Topic 和 Consumer Group ==========
echo ""
echo ">>> Step 3: 检查/创建 Topic 和 Consumer Group..."
cd "$ROCKETMQ_HOME"

# 获取当前 topic 列表
EXISTING_TOPICS=$(bin/mqadmin topicList -n localhost:9876 2>/dev/null)

# 检查并创建所有需要的 Topic（NORMAL 类型）
for TOPIC in stream-demo-topic stream-demo-topic2 stream-transform-topic; do
  if echo "$EXISTING_TOPICS" | grep -qw "$TOPIC"; then
    echo "✓ $TOPIC 已存在"
  else
    bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t "$TOPIC" -a +message.type=NORMAL 2>/dev/null
    echo "✓ $TOPIC 已创建"
  fi
done

# 检查并创建 DELAY 类型 Topic
if echo "$EXISTING_TOPICS" | grep -qw "stream-delay-topic"; then
  echo "✓ stream-delay-topic 已存在"
else
  bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-delay-topic -a +message.type=DELAY 2>/dev/null
  echo "✓ stream-delay-topic 已创建 (DELAY)"
fi

# 检查并创建 FIFO 类型 Topic
if echo "$EXISTING_TOPICS" | grep -qw "stream-fifo-topic"; then
  echo "✓ stream-fifo-topic 已存在"
else
  bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-fifo-topic -a +message.type=FIFO 2>/dev/null
  echo "✓ stream-fifo-topic 已创建 (FIFO)"
fi

# 检查并创建 TRANSACTION 类型 Topic
if echo "$EXISTING_TOPICS" | grep -qw "stream-tx-topic"; then
  echo "✓ stream-tx-topic 已存在"
else
  bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-tx-topic -a +message.type=TRANSACTION 2>/dev/null
  echo "✓ stream-tx-topic 已创建 (TRANSACTION)"
fi

# 检查并创建所有需要的 Consumer Group
for GROUP in stream-demo-consumer-group stream-demo-consumer-group2 stream-transform-group stream-delay-group stream-fifo-group stream-tx-group; do
  if bin/mqadmin consumerProgress -n localhost:9876 -g "$GROUP" >/dev/null 2>&1; then
    echo "✓ $GROUP 已存在"
  else
    bin/mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g "$GROUP" 2>/dev/null
    echo "✓ $GROUP 已创建"
  fi
done

# ========== Step 4: 打包并启动 Stream 模块 ==========
echo ""
echo ">>> Step 4: 打包并启动 Stream 模块..."
cd "$PROJECT_DIR"

# 打包（如果 jar 不存在或代码有更新）
echo "打包 cloud-stream-sample..."
./mvnw -pl cloud-stream-sample -am package -DskipTests -q
echo "✓ 打包完成"

# 启动
java -jar cloud-stream-sample/target/cloud-stream-sample.jar > logs/stream-sample.log 2>&1 &
STREAM_PID=$!
echo "Stream 模块启动中 (PID: $STREAM_PID)..."

# 等待启动（通过 actuator 健康检查）
for i in $(seq 1 60); do
  if curl -s "http://127.0.0.1:8767/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    echo "✓ Stream 模块已就绪 (${i}s)"
    break
  fi
  if [ "$i" = "60" ]; then
    echo "✗ Stream 模块启动超时"
    tail -30 logs/stream-sample.log
    exit 1
  fi
  sleep 1
done

# ========== Step 5: 验证消息收发 ==========
echo ""
echo "=========================================="
echo "  场景1: 基础消费 (StreamBridge → input)"
echo "=========================================="
sleep 3

if grep -q "Received message: Hello" logs/stream-sample.log; then
  echo "✓ stream-demo-topic 消息消费正常"
  grep "Received message: Hello" logs/stream-sample.log | tail -1
else
  echo "✗ 未收到 Hello 消息"
fi

echo ""
echo "=========================================="
echo "  场景2: 定时消息源 (Supplier → input2)"
echo "=========================================="
# 等待至少一个定时周期（10s），确保 Supplier 已发送消息
sleep 10

if grep -q "收到消息: 你好" logs/stream-sample.log; then
  echo "✓ stream-demo-topic2 定时消息消费正常"
  grep "收到消息: 你好" logs/stream-sample.log | tail -3
else
  echo "✗ 未收到 你好 消息"
fi

echo ""
echo "=========================================="
echo "  场景3: 消息处理管道 (REST → transform → input2)"
echo "=========================================="

# 通过 REST API 发送消息到 transform 管道
echo "发送消息到 transform 管道..."
curl -s -X POST "http://127.0.0.1:8767/stream/send?message=hello+pipeline"
echo ""
sleep 3

# 检查 transform 函数是否执行了转换
if grep -q "消息转换: hello pipeline -> \[PROCESSED\] HELLO PIPELINE" logs/stream-sample.log; then
  echo "✓ 消息转换管道工作正常"
  grep "消息转换:" logs/stream-sample.log | tail -1
else
  echo "✗ 消息转换未执行"
fi

echo ""
echo "=========================================="
echo "  场景4: 延迟消息 (REST → delay-topic → delay Consumer)"
echo "=========================================="

echo "发送延迟消息 (delayLevel=2, 约5秒后投递)..."
SEND_TIME=$(date +"%H:%M:%S.%N")
echo "发送时间: $SEND_TIME"
curl -s -X POST "http://127.0.0.1:8767/stream/delay?message=hello+delay&delayLevel=2"
echo ""
sleep 8

if grep -q "\[延迟消息\] 收到: hello delay" logs/stream-sample.log; then
  echo "✓ 延迟消息消费正常"
  echo "--- 发送日志 ---"
  grep "发送延迟消息: hello delay" logs/stream-sample.log | tail -1
  echo "--- 接收日志 ---"
  grep "\[延迟消息\]" logs/stream-sample.log | tail -1
else
  echo "✗ 未收到延迟消息"
fi

echo ""
echo "=========================================="
echo "  场景5: 顺序消息 (REST → fifo-topic → fifo Consumer)"
echo "=========================================="

echo "发送10条顺序消息 (相同 orderKey)..."
for i in $(seq 1 10); do
  curl -s -X POST "http://127.0.0.1:8767/stream/fifo?message=order-msg-${i}&orderKey=order-1"
  echo ""
done
sleep 5

FIFO_COUNT=$(grep -c "\[顺序消息\] 收到: order-msg-" logs/stream-sample.log 2>/dev/null || echo "0")
if [ "$FIFO_COUNT" -ge 10 ]; then
  echo "✓ 顺序消息消费正常 (${FIFO_COUNT} 条)"
  echo "--- 接收顺序 ---"
  grep "\[顺序消息\]" logs/stream-sample.log | tail -10
else
  echo "✗ 顺序消息消费不足 (期望≥10, 实际${FIFO_COUNT})"
fi

echo ""
echo "=========================================="
echo "  场景6: 事务消息 (REST → tx-topic → tx Consumer)"
echo "=========================================="

echo "发送事务消息 (arg=commit, 本地事务提交)..."
curl -s -X POST "http://127.0.0.1:8767/stream/tx?message=hello+tx&arg=commit"
echo ""
sleep 3

if grep -q "\[事务消息\] 收到: hello tx" logs/stream-sample.log; then
  echo "✓ 事务消息消费正常 (提交)"
  echo "--- 事务执行日志 ---"
  grep "\[事务消息\] 执行本地事务" logs/stream-sample.log | tail -1
  echo "--- 接收日志 ---"
  grep "\[事务消息\] 收到" logs/stream-sample.log | tail -1
else
  echo "✗ 未收到事务消息 (提交)"
fi

echo ""
echo "发送事务消息 (arg=rollback, 本地事务回滚)..."
# 清空日志中的事务消息记录以便验证回滚
BEFORE_COUNT=$(grep -c "\[事务消息\] 收到:" logs/stream-sample.log 2>/dev/null || echo "0")
curl -s -X POST "http://127.0.0.1:8767/stream/tx?message=hello+rollback&arg=rollback"
echo ""
sleep 3

AFTER_COUNT=$(grep -c "\[事务消息\] 收到:" logs/stream-sample.log 2>/dev/null || echo "0")
if grep -q "\[事务消息\] 本地事务回滚" logs/stream-sample.log; then
  echo "✓ 事务消息回滚正常 (消费者未收到回滚的消息)"
  grep "\[事务消息\] 本地事务回滚" logs/stream-sample.log | tail -1
else
  echo "✗ 事务消息回滚异常"
fi

# ========== Step 6: 清理 ==========
echo ""
echo "=========================================="
echo "  验证完成，清理..."
echo "=========================================="
pkill -f "cloud-stream-sample" 2>/dev/null && echo "✓ Stream 模块已停止" || echo "Stream 模块未运行"
pkill -f "rocketmq" 2>/dev/null; sleep 1
pgrep -f "rocketmq" | xargs kill -9 2>/dev/null; sleep 1
nc -z 127.0.0.1 9876 2>/dev/null && echo "✗ RocketMQ NameServer 仍在运行" || echo "✓ RocketMQ NameServer 已停止"
nc -z 127.0.0.1 10911 2>/dev/null && echo "✗ RocketMQ Broker 仍在运行" || echo "✓ RocketMQ Broker 已停止"

echo ""
echo "=========================================="
echo "  全部完成！"
echo "=========================================="
