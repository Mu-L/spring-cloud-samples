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

# 检查并创建 stream-demo-topic
if echo "$EXISTING_TOPICS" | grep -qw "stream-demo-topic"; then
  echo "✓ stream-demo-topic 已存在"
else
  bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic -a +message.type=NORMAL 2>/dev/null
  echo "✓ stream-demo-topic 已创建"
fi

# 检查并创建 stream-demo-topic2
if echo "$EXISTING_TOPICS" | grep -qw "stream-demo-topic2"; then
  echo "✓ stream-demo-topic2 已存在"
else
  bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-demo-topic2 -a +message.type=NORMAL 2>/dev/null
  echo "✓ stream-demo-topic2 已创建"
fi

# 检查并创建 consumer group（通过 consumerProgress 判断）
for GROUP in stream-demo-consumer-group stream-demo-consumer-group2; do
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

# 打包（如果 jar 不存在）
if [ ! -f cloud-stream-sample/target/cloud-stream-sample.jar ]; then
  echo "打包 cloud-stream-sample..."
  ./mvnw -pl cloud-stream-sample -am package -DskipTests -q
  echo "✓ 打包完成"
fi

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
echo "  验证消息消费"
echo "=========================================="
sleep 5

echo ""
echo "=== stream-demo-topic ==="
if grep -q "Received message: Hello" logs/stream-sample.log; then
  echo "✓ stream-demo-topic 消息消费正常"
  grep "Received message: Hello" logs/stream-sample.log | tail -3
else
  echo "✗ 未收到 Hello 消息"
  echo "最近日志:"
  tail -20 logs/stream-sample.log
fi

echo ""
echo "=== stream-demo-topic2 ==="
if grep -q "收到消息: 你好" logs/stream-sample.log; then
  echo "✓ stream-demo-topic2 消息消费正常"
  grep "收到消息: 你好" logs/stream-sample.log | tail -3
else
  echo "✗ 未收到 你好 消息"
  echo "最近日志:"
  tail -20 logs/stream-sample.log
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
