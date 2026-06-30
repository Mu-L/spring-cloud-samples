#!/bin/bash
# Trace 链路追踪验证脚本
# 用法: bash .qoder/skills/demo-spring-cloud/verify-trace.sh
# 覆盖五条链路:
#   1. Web         → Web        : consumer-sample        → provider-sample        (RestTemplate v1.0 / FeignClient v2.0, trace 均自动传播)
#   2. Web         → gRPC       : consumer-sample        → grpc-server-sample     (gRPC)
#   3. Web         → Dubbo      : consumer-sample        → provider-dubbo-sample  (Dubbo)
#   4. Reactive Web→ Reactive Web: consumer-reactive-sample → provider-reactive-sample (WebClient, 手动传递 traceparent)
#   5. Reactive Web→ Dubbo      : consumer-reactive-sample → provider-dubbo-sample  (Dubbo)
# 前提: provider-sample, provider-reactive, provider-dubbo, grpc-server, consumer-sample, consumer-reactive-sample 已启动

# 自动检测项目根目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
LOG_DIR="$PROJECT_ROOT/logs"

VERIFY_PASS=0
VERIFY_FAIL=0

pass() { VERIFY_PASS=$((VERIFY_PASS + 1)); echo "  ✓ $1"; }
fail() { VERIFY_FAIL=$((VERIFY_FAIL + 1)); echo "  ✗ $1"; }

# 从日志中提取指定 trace ID 的行（从尾部搜索，取最新一条）
# 参数: $1=日志文件, $2=trace ID
extract_trace() {
  # macOS 没有 tac，使用 tail -r 替代
  if command -v tac &>/dev/null; then
    tac "$1" 2>/dev/null | grep -m1 "$2"
  else
    tail -r "$1" 2>/dev/null | grep -m1 "$2"
  fi
}

# 生成符合 W3C 标准的 32 位十六进制 trace ID
gen_trace_id() {
  printf '%08x%08x%08x%08x' $RANDOM $RANDOM $RANDOM $RANDOM
}

# 从日志行中提取 trace ID（Spring Boot 日志格式: [traceId-spanId]）
# 参数: $1=日志行
get_trace_id() {
  echo "$1" | grep -o '\[[a-f0-9]\{8,\}-[a-f0-9]*\]' | head -1 | tr -d '[]' | cut -d'-' -f1
}

echo "=========================================="
echo "  Trace 链路追踪验证（五条链路）"
echo "=========================================="

# ========== Step 1: 检查前置服务 ==========
echo ""
echo ">>> Step 1: 检查前置服务..."

ALL_OK=true

# consumer-sample (8766) — 链路 1/2/3 的入口
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8766/actuator/health" 2>/dev/null | grep -q "200"; then
  pass "consumer-sample (8766) 已运行 [链路 1/2/3 入口]"
else
  fail "consumer-sample (8766) 未就绪"
  ALL_OK=false
fi

# consumer-reactive-sample (8763) — 链路 4/5 的入口
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8763/actuator/health" 2>/dev/null | grep -q "200"; then
  pass "consumer-reactive-sample (8763) 已运行 [链路 4/5 入口]"
else
  fail "consumer-reactive-sample (8763) 未就绪"
  ALL_OK=false
fi

# provider-sample (8765) — 链路 1 目标
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8765/actuator/health" 2>/dev/null | grep -q "200"; then
  pass "provider-sample (8765) 已运行 [链路 1 目标]"
else
  fail "provider-sample (8765) 未就绪 [链路 1 需要]"
  ALL_OK=false
fi

# provider-reactive-sample (8762) — 链路 4 目标
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8762/actuator/health" 2>/dev/null | grep -q "200"; then
  pass "provider-reactive-sample (8762) 已运行 [链路 4 目标]"
else
  fail "provider-reactive-sample (8762) 未就绪 [链路 4 需要]"
  ALL_OK=false
fi

# grpc-server-sample (8090) — 链路 2 目标
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8090/actuator/health" 2>/dev/null | grep -q "200"; then
  pass "grpc-server-sample (8090) 已运行 [链路 2 目标]"
else
  fail "grpc-server-sample (8090) 未就绪 [链路 2 需要]"
  ALL_OK=false
fi

# provider-dubbo-sample — 链路 3/5 目标（无 HTTP 端口，通过 Nacos 注册验证）
# Nacos 3.x 不支持 v1 API，改用 nacos-discovery-sample 的 /discovery/services 端点
if curl -s "http://localhost:8760/discovery/services" 2>/dev/null | grep -q "provider-dubbo-sample"; then
  pass "provider-dubbo-sample 已注册到 Nacos [链路 3/5 目标]"
else
  fail "provider-dubbo-sample 未在 Nacos 中注册 [链路 3/5 需要]"
  ALL_OK=false
fi

if [ "$ALL_OK" = false ]; then
  echo ""
  echo "✗ 请先启动相关服务: sh start-all.sh 或 ./mvnw -pl <模块> spring-boot:run"
  exit 1
fi

# ========== 检查日志文件可用性 ==========
echo ""
echo ">>> 检查日志文件..."

CONSUMER_LOG=""
CONSUMER_REACTIVE_LOG=""
PROVIDER_LOG=""
PROVIDER_REACTIVE_LOG=""
GRPC_SERVER_LOG=""
DUBBO_LOG=""

# 动态发现日志文件: 通过 lsof 从运行中进程的 fd 获取实际日志路径
# 参数: $1=端口号
find_log_by_port() {
  local port="$1"
  local pid=$(lsof -i :"$port" -t 2>/dev/null | head -1)
  if [ -z "$pid" ]; then return; fi
  local logfile=$(lsof -p "$pid" 2>/dev/null | grep "1w.*REG" | awk '{print $NF}' | head -1)
  if [ -n "$logfile" ] && [ -f "$logfile" ]; then
    echo "$logfile"
  fi
}

# 优先方案 1: 通过 lsof 动态发现（最可靠，适配日志文件被删除重建等场景）
DYNAMIC_OK=true
CONSUMER_LOG=$(find_log_by_port 8766)       || DYNAMIC_OK=false
CONSUMER_REACTIVE_LOG=$(find_log_by_port 8763) || DYNAMIC_OK=false
PROVIDER_LOG=$(find_log_by_port 8765)       || DYNAMIC_OK=false
PROVIDER_REACTIVE_LOG=$(find_log_by_port 8762) || DYNAMIC_OK=false
GRPC_SERVER_LOG=$(find_log_by_port 8090)    || DYNAMIC_OK=false

# provider-dubbo 无 HTTP 端口，通过 start-all.sh 日志或 /tmp 查找
if [ -f "$LOG_DIR/provider-dubbo.log" ]; then
  DUBBO_LOG="$LOG_DIR/provider-dubbo.log"
elif [ -f "/tmp/provider-dubbo.log" ]; then
  DUBBO_LOG="/tmp/provider-dubbo.log"
fi

if [ "$DYNAMIC_OK" = true ] && [ -n "$CONSUMER_LOG" ]; then
  echo "  ✓ 通过进程 fd 动态发现日志文件"
  echo "    consumer-sample  → $CONSUMER_LOG"
  echo "    consumer-reactive→ $CONSUMER_REACTIVE_LOG"
  echo "    provider-sample  → $PROVIDER_LOG"
  echo "    provider-reactive→ $PROVIDER_REACTIVE_LOG"
  echo "    grpc-server      → $GRPC_SERVER_LOG"
  [ -n "$DUBBO_LOG" ] && echo "    provider-dubbo   → $DUBBO_LOG"
# 优先方案 2: start-all.sh 日志目录
elif [ -f "$LOG_DIR/consumer.log" ]; then
  CONSUMER_LOG="$LOG_DIR/consumer.log"
  CONSUMER_REACTIVE_LOG="$LOG_DIR/consumer-reactive.log"
  PROVIDER_LOG="$LOG_DIR/provider.log"
  PROVIDER_REACTIVE_LOG="$LOG_DIR/provider-reactive.log"
  GRPC_SERVER_LOG="$LOG_DIR/grpc-server.log"
  DUBBO_LOG="$LOG_DIR/provider-dubbo.log"
  echo "  ✓ 找到 start-all.sh 日志目录: $LOG_DIR"
# 优先方案 3: /tmp 日志文件
elif [ -f "/tmp/consumer-sample.log" ]; then
  CONSUMER_LOG="/tmp/consumer-sample.log"
  CONSUMER_REACTIVE_LOG="/tmp/consumer-reactive-sample.log"
  PROVIDER_LOG="/tmp/provider-sample.log"
  PROVIDER_REACTIVE_LOG="/tmp/provider-reactive-sample.log"
  GRPC_SERVER_LOG="/tmp/grpc-server-sample.log"
  DUBBO_LOG="/tmp/provider-dubbo.log"
  echo "  ✓ 找到 /tmp 日志文件"
else
  echo "  ⚠ 未找到日志文件，仅验证接口调用，跳过 trace ID 传播检查"
  echo "    提示: 使用 start-all.sh 启动可自动验证 trace ID 传播"
fi

# ============================================================
# Step 2: 链路 1 — Web → Web (consumer-sample → provider-sample)
#   v1.0 (默认): RestTemplate
#   v2.0 (?version=2.0): FeignClient
#   两种客户端 trace 均自动传播
# ============================================================
echo ""
echo ">>> Step 2: 链路 1 — Web → Web (consumer-sample → provider-sample via RestTemplate/FeignClient)..."

# --- 2a: RestTemplate (v1.0, 默认) ---
echo ""
echo "  [2a] RestTemplate (v1.0)..."

TRACE_ID_WEB=$(gen_trace_id)
SPAN_ID_WEB="$(printf '%016x' $RANDOM)"

RESP_WEB=$(curl -s -H "traceparent: 00-${TRACE_ID_WEB}-${SPAN_ID_WEB}-01" \
  "http://localhost:8766/hi?name=traceWeb")

if echo "$RESP_WEB" | grep -q "Hi,"; then
  pass "consumer-sample → provider-sample (RestTemplate) 调用成功"
else
  fail "consumer-sample → provider-sample (RestTemplate) 调用失败: $RESP_WEB"
fi

sleep 1

if [ -n "$CONSUMER_LOG" ] && [ -f "$CONSUMER_LOG" ]; then
  CONSUMER_LINE_WEB=$(extract_trace "$CONSUMER_LOG" "$TRACE_ID_WEB")
  CONSUMER_TID_WEB=$(get_trace_id "$CONSUMER_LINE_WEB")
  if [ "$CONSUMER_TID_WEB" = "$TRACE_ID_WEB" ]; then
    pass "consumer-sample 日志包含 trace ID: $TRACE_ID_WEB"
  else
    fail "consumer-sample 日志未找到 trace ID: $TRACE_ID_WEB"
  fi
fi

if [ -n "$PROVIDER_LOG" ] && [ -f "$PROVIDER_LOG" ]; then
  PROVIDER_LINE_WEB=$(extract_trace "$PROVIDER_LOG" "$TRACE_ID_WEB")
  PROVIDER_TID_WEB=$(get_trace_id "$PROVIDER_LINE_WEB")
  if [ "$PROVIDER_TID_WEB" = "$TRACE_ID_WEB" ]; then
    pass "provider-sample 日志包含相同 trace ID: $TRACE_ID_WEB (RestTemplate trace 传播 ✓)"
  else
    fail "provider-sample 日志未找到 trace ID: $TRACE_ID_WEB (RestTemplate trace 传播失败)"
  fi
fi

# --- 2b: FeignClient (v2.0, ?version=2.0) ---
echo ""
echo "  [2b] FeignClient (v2.0)..."

TRACE_ID_FEIGN=$(gen_trace_id)
SPAN_ID_FEIGN="$(printf '%016x' $RANDOM)"

RESP_FEIGN=$(curl -s -H "traceparent: 00-${TRACE_ID_FEIGN}-${SPAN_ID_FEIGN}-01" \
  "http://localhost:8766/hi?name=traceFeign&version=2.0")

if echo "$RESP_FEIGN" | grep -q "Hi,"; then
  pass "consumer-sample → provider-sample (FeignClient) 调用成功"
else
  fail "consumer-sample → provider-sample (FeignClient) 调用失败: $RESP_FEIGN"
fi

sleep 1

if [ -n "$CONSUMER_LOG" ] && [ -f "$CONSUMER_LOG" ]; then
  CONSUMER_LINE_FEIGN=$(extract_trace "$CONSUMER_LOG" "$TRACE_ID_FEIGN")
  CONSUMER_TID_FEIGN=$(get_trace_id "$CONSUMER_LINE_FEIGN")
  if [ "$CONSUMER_TID_FEIGN" = "$TRACE_ID_FEIGN" ]; then
    pass "consumer-sample 日志包含 trace ID: $TRACE_ID_FEIGN"
  else
    fail "consumer-sample 日志未找到 trace ID: $TRACE_ID_FEIGN"
  fi
fi

if [ -n "$PROVIDER_LOG" ] && [ -f "$PROVIDER_LOG" ]; then
  PROVIDER_LINE_FEIGN=$(extract_trace "$PROVIDER_LOG" "$TRACE_ID_FEIGN")
  PROVIDER_TID_FEIGN=$(get_trace_id "$PROVIDER_LINE_FEIGN")
  if [ "$PROVIDER_TID_FEIGN" = "$TRACE_ID_FEIGN" ]; then
    pass "provider-sample 日志包含相同 trace ID: $TRACE_ID_FEIGN (FeignClient trace 传播 ✓)"
  else
    fail "provider-sample 日志未找到 trace ID: $TRACE_ID_FEIGN (FeignClient trace 传播失败)"
  fi
fi

# ============================================================
# Step 3: 链路 2 — Web → gRPC (consumer-sample → grpc-server-sample)
# ============================================================
echo ""
echo ">>> Step 3: 链路 2 — Web → gRPC (consumer-sample → grpc-server-sample via gRPC)..."

TRACE_ID_GRPC=$(gen_trace_id)
SPAN_ID_GRPC="$(printf '%016x' $RANDOM)"

RESP_GRPC=$(curl -s -H "traceparent: 00-${TRACE_ID_GRPC}-${SPAN_ID_GRPC}-01" \
  "http://localhost:8766/grpc?name=traceGrpc")

if echo "$RESP_GRPC" | grep -qi "hello\|Hello"; then
  pass "consumer-sample → grpc-server 调用成功"
else
  fail "consumer-sample → grpc-server 调用失败: $RESP_GRPC"
fi

sleep 1

if [ -n "$CONSUMER_LOG" ] && [ -f "$CONSUMER_LOG" ]; then
  CONSUMER_LINE_GRPC=$(extract_trace "$CONSUMER_LOG" "$TRACE_ID_GRPC")
  CONSUMER_TID_GRPC=$(get_trace_id "$CONSUMER_LINE_GRPC")
  if [ "$CONSUMER_TID_GRPC" = "$TRACE_ID_GRPC" ]; then
    pass "consumer-sample 日志包含 trace ID: $TRACE_ID_GRPC"
  else
    fail "consumer-sample 日志未找到 trace ID: $TRACE_ID_GRPC"
  fi
fi

if [ -n "$GRPC_SERVER_LOG" ] && [ -f "$GRPC_SERVER_LOG" ]; then
  GRPC_LINE=$(extract_trace "$GRPC_SERVER_LOG" "$TRACE_ID_GRPC")
  GRPC_TID=$(get_trace_id "$GRPC_LINE")
  if [ "$GRPC_TID" = "$TRACE_ID_GRPC" ]; then
    pass "grpc-server 日志包含相同 trace ID: $TRACE_ID_GRPC (gRPC trace 传播 ✓)"
  else
    fail "grpc-server 日志未找到 trace ID: $TRACE_ID_GRPC (gRPC trace 传播失败)"
  fi
fi

# ============================================================
# Step 4: 链路 3 — Web → Dubbo (consumer-sample → provider-dubbo-sample)
# ============================================================
echo ""
echo ">>> Step 4: 链路 3 — Web → Dubbo (consumer-sample → provider-dubbo-sample via Dubbo)..."

TRACE_ID_DUBBO=$(gen_trace_id)
SPAN_ID_DUBBO="$(printf '%016x' $RANDOM)"

RESP_DUBBO=$(curl -s -H "traceparent: 00-${TRACE_ID_DUBBO}-${SPAN_ID_DUBBO}-01" \
  "http://localhost:8766/dubbo?name=traceDubbo")

if echo "$RESP_DUBBO" | grep -q "Hello"; then
  pass "consumer-sample → provider-dubbo 调用成功"
else
  fail "consumer-sample → provider-dubbo 调用失败: $RESP_DUBBO"
fi

sleep 1

if [ -n "$CONSUMER_LOG" ] && [ -f "$CONSUMER_LOG" ]; then
  CONSUMER_LINE_DUBBO=$(extract_trace "$CONSUMER_LOG" "$TRACE_ID_DUBBO")
  CONSUMER_TID_DUBBO=$(get_trace_id "$CONSUMER_LINE_DUBBO")
  if [ "$CONSUMER_TID_DUBBO" = "$TRACE_ID_DUBBO" ]; then
    pass "consumer-sample 日志包含 trace ID: $TRACE_ID_DUBBO"
  else
    fail "consumer-sample 日志未找到 trace ID: $TRACE_ID_DUBBO"
  fi
fi

if [ -n "$DUBBO_LOG" ] && [ -f "$DUBBO_LOG" ]; then
  DUBBO_LINE=$(extract_trace "$DUBBO_LOG" "$TRACE_ID_DUBBO")
  DUBBO_TID=$(get_trace_id "$DUBBO_LINE")
  if [ "$DUBBO_TID" = "$TRACE_ID_DUBBO" ]; then
    pass "provider-dubbo 日志包含相同 trace ID: $TRACE_ID_DUBBO (Dubbo trace 传播 ✓)"
  else
    fail "provider-dubbo 日志未找到 trace ID: $TRACE_ID_DUBBO (Dubbo trace 传播失败)"
  fi
fi

# ============================================================
# Step 5: 链路 4 — Reactive Web → Reactive Web
#         (consumer-reactive-sample → provider-reactive-sample via WebClient)
# 注意: Reactive WebClient 链路需手动传递 traceparent header（与 RestTemplate 自动传播不同）
# ============================================================
echo ""
echo ">>> Step 5: 链路 4 — Reactive Web → Reactive Web (consumer-reactive → provider-reactive via WebClient)..."

TRACE_ID_RWEB=$(gen_trace_id)
SPAN_ID_RWEB="$(printf '%016x' $RANDOM)"

RESP_RWEB=$(curl -s -H "traceparent: 00-${TRACE_ID_RWEB}-${SPAN_ID_RWEB}-01" \
  "http://localhost:8763/hi?name=traceRWeb")

if echo "$RESP_RWEB" | grep -q "Hi,"; then
  pass "consumer-reactive → provider-reactive 调用成功"
else
  fail "consumer-reactive → provider-reactive 调用失败: $RESP_RWEB"
fi

sleep 1

if [ -n "$CONSUMER_REACTIVE_LOG" ] && [ -f "$CONSUMER_REACTIVE_LOG" ]; then
  RCONSUMER_LINE=$(extract_trace "$CONSUMER_REACTIVE_LOG" "$TRACE_ID_RWEB")
  RCONSUMER_TID=$(get_trace_id "$RCONSUMER_LINE")
  if [ "$RCONSUMER_TID" = "$TRACE_ID_RWEB" ]; then
    pass "consumer-reactive 日志包含 trace ID: $TRACE_ID_RWEB"
  else
    fail "consumer-reactive 日志未找到 trace ID: $TRACE_ID_RWEB"
  fi
fi

if [ -n "$PROVIDER_REACTIVE_LOG" ] && [ -f "$PROVIDER_REACTIVE_LOG" ]; then
  RPROVIDER_LINE=$(extract_trace "$PROVIDER_REACTIVE_LOG" "$TRACE_ID_RWEB")
  RPROVIDER_TID=$(get_trace_id "$RPROVIDER_LINE")
  if [ "$RPROVIDER_TID" = "$TRACE_ID_RWEB" ]; then
    pass "provider-reactive 日志包含相同 trace ID: $TRACE_ID_RWEB (Reactive Web trace 传播 ✓)"
  else
    fail "provider-reactive 日志未找到 trace ID: $TRACE_ID_RWEB (Reactive Web trace 传播失败)"
  fi
fi

# ============================================================
# Step 6: 链路 5 — Reactive Web → Dubbo
#         (consumer-reactive-sample → provider-dubbo-sample via Dubbo)
# ============================================================
echo ""
echo ">>> Step 6: 链路 5 — Reactive Web → Dubbo (consumer-reactive → provider-dubbo via Dubbo)..."

TRACE_ID_RDUBBO=$(gen_trace_id)
SPAN_ID_RDUBBO="$(printf '%016x' $RANDOM)"

RESP_RDUBBO=$(curl -s -H "traceparent: 00-${TRACE_ID_RDUBBO}-${SPAN_ID_RDUBBO}-01" \
  "http://localhost:8763/dubbo?name=traceRDubbo")

if echo "$RESP_RDUBBO" | grep -q "Hello"; then
  pass "consumer-reactive → provider-dubbo 调用成功"
else
  fail "consumer-reactive → provider-dubbo 调用失败: $RESP_RDUBBO"
fi

sleep 1

if [ -n "$CONSUMER_REACTIVE_LOG" ] && [ -f "$CONSUMER_REACTIVE_LOG" ]; then
  RCONSUMER_LINE2=$(extract_trace "$CONSUMER_REACTIVE_LOG" "$TRACE_ID_RDUBBO")
  RCONSUMER_TID2=$(get_trace_id "$RCONSUMER_LINE2")
  if [ "$RCONSUMER_TID2" = "$TRACE_ID_RDUBBO" ]; then
    pass "consumer-reactive 日志包含 trace ID: $TRACE_ID_RDUBBO"
  else
    fail "consumer-reactive 日志未找到 trace ID: $TRACE_ID_RDUBBO"
  fi
fi

if [ -n "$DUBBO_LOG" ] && [ -f "$DUBBO_LOG" ]; then
  RDUBBO_LINE=$(extract_trace "$DUBBO_LOG" "$TRACE_ID_RDUBBO")
  RDUBBO_TID=$(get_trace_id "$RDUBBO_LINE")
  if [ "$RDUBBO_TID" = "$TRACE_ID_RDUBBO" ]; then
    pass "provider-dubbo 日志包含相同 trace ID: $TRACE_ID_RDUBBO (Reactive→Dubbo trace 传播 ✓)"
  else
    fail "provider-dubbo 日志未找到 trace ID: $TRACE_ID_RDUBBO (Reactive→Dubbo trace 传播失败)"
  fi
fi

# ============================================================
# Step 7: 无 traceparent header 的自动 trace 生成验证
# ============================================================
echo ""
echo ">>> Step 7: 自动 trace 生成验证（不传 traceparent header）..."

RESP_AUTO=$(curl -s "http://localhost:8766/dubbo?name=autoTrace")
if echo "$RESP_AUTO" | grep -q "Hello"; then
  pass "无 traceparent 时调用正常（Spring Boot 自动生成 trace context）"
else
  fail "无 traceparent 时调用失败: $RESP_AUTO"
fi

# ============================================================
# 汇总结果
# ============================================================
echo ""
echo "=========================================="
echo "  验证结果: 通过 $VERIFY_PASS 项, 失败 $VERIFY_FAIL 项"
echo "=========================================="
echo ""
echo "  链路汇总:"
echo "    1. Web → Web         : consumer-sample        → provider-sample        (RestTemplate/FeignClient)"
echo "    2. Web → gRPC        : consumer-sample        → grpc-server-sample     (gRPC)"
echo "    3. Web → Dubbo       : consumer-sample        → provider-dubbo-sample  (Dubbo)"
echo "    4. Reactive → Reactive: consumer-reactive-sample → provider-reactive-sample (WebClient, 手动传递)"
echo "    5. Reactive → Dubbo  : consumer-reactive-sample → provider-dubbo-sample  (Dubbo)"
echo ""

if [ "$VERIFY_FAIL" -eq 0 ]; then
  echo "  ★ 全部验证通过! 五条链路 Trace 传播均正常 ★"
else
  echo "  存在失败项，请检查各模块日志和 tracing 配置"
  exit 1
fi
