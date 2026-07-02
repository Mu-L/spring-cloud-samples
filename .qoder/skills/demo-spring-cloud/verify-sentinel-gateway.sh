#!/bin/bash
# Sentinel 网关限流验证脚本
# 用法: bash .qoder/skills/demo-spring-cloud/verify-sentinel-gateway.sh
# 自动启动: cloud-gateway-sample(8764), cloud-consumer-sample(8766),
#           cloud-provider-sample(8765), cloud-nacos-config-sample(8761)

cd "$(dirname "$0")/../../.."
PROJECT_DIR=$(pwd)
mkdir -p logs .pids

NACOS_CONFIG_URL="http://localhost:8761/nacos"
GATEWAY_URL="http://localhost:8764"

echo "=========================================="
echo "  Sentinel 网关限流验证"
echo "=========================================="

# ========== Step 1: 检查/启动前置服务 ==========
echo ""
echo ">>> Step 1: 检查/启动前置服务..."

# 1a: 检查 Nacos
if curl -s -o /dev/null -w '' "http://127.0.0.1:8848/nacos/actuator/health" 2>/dev/null; then
  echo "  ✓ Nacos 已运行"
else
  echo "  ✗ Nacos 未运行，请先启动 Nacos"
  exit 1
fi

# 辅助函数: 检查/启动服务
check_or_start() {
  local name="$1" port="$2" module_dir="$3" jar_name="$4" log_name="$5" pid_name="$6"
  if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null | grep -q "200"; then
    echo "  ✓ $name ($port) 已运行"
  else
    echo "  → $name ($port) 未运行，正在启动..."
    if [ ! -f "$module_dir/target/$jar_name" ]; then
      echo "  打包 $module_dir..."
      ./mvnw -pl "$module_dir" -am package -DskipTests -q
    fi
    java -jar "$module_dir/target/$jar_name" > "logs/$log_name.log" 2>&1 &
    echo $! > ".pids/$pid_name.pid"
    echo "  $name 启动中 (PID: $!)..."
    for i in $(seq 1 60); do
      if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null | grep -q "200"; then
        echo "  ✓ $name ($port) 已就绪 (${i}s)"
        return
      fi
      if [ $i -eq 60 ]; then
        echo "  ✗ $name 启动超时，请查看 logs/$log_name.log"
        exit 1
      fi
      sleep 1
    done
  fi
}

# 按依赖顺序启动: nacos-config → provider → consumer → gateway
check_or_start "NacosConfig" 8761 "cloud-nacos-config-sample" "cloud-nacos-config-sample.jar" "nacos-config" "nacos-config"
check_or_start "Provider"     8765 "cloud-provider-sample"      "cloud-provider-sample.jar"      "provider"     "provider"
check_or_start "Consumer"     8766 "cloud-consumer-sample"       "cloud-consumer-sample.jar"       "consumer"     "consumer"
check_or_start "Gateway"      8764 "cloud-gateway-sample"        "cloud-gateway-sample.jar"        "gateway"      "gateway"

# ========== Step 2: 发布 Sentinel 配置 ==========
echo ""
echo ">>> Step 2: 发布 Sentinel 限流配置..."

# gw-api-group
API_GROUP_JSON='[
  {"apiName":"consumer_reactive_api","predicateItems":[{"pattern":"/consumer-reactive-sample/**","matchStrategy":1}]},
  {"apiName":"consumer_api","predicateItems":[{"pattern":"/consumer-sample/**","matchStrategy":1}]}
]'

curl -s -X POST "$NACOS_CONFIG_URL/publishConfig" \
  --data-urlencode 'dataId=cloud.sample.gateway.gw-api-group' \
  --data-urlencode 'group=SENTINEL_GROUP' \
  --data-urlencode 'type=json' \
  --data-urlencode "content=$API_GROUP_JSON" > /dev/null
echo "  ✓ 已发布 gw-api-group"

# gw-flow
FLOW_JSON='[
  {"resource":"consumer_reactive_api","resourceMode":1,"count":10},
  {"resource":"consumer_api","resourceMode":1,"count":5},
  {"resource":"consumer-reactive-sample","resourceMode":0,"count":20}
]'

curl -s -X POST "$NACOS_CONFIG_URL/publishConfig" \
  --data-urlencode 'dataId=cloud.sample.gateway.gw-flow' \
  --data-urlencode 'group=SENTINEL_GROUP' \
  --data-urlencode 'type=json' \
  --data-urlencode "content=$FLOW_JSON" > /dev/null
echo "  ✓ 已发布 gw-flow"

# 等待 Nacos 配置同步
echo "  等待配置同步..."
sleep 2

# ========== Step 3: 验证配置写入 ==========
echo ""
echo ">>> Step 3: 验证配置写入..."

API_RESULT=$(curl -s "$NACOS_CONFIG_URL/getConfig?dataId=cloud.sample.gateway.gw-api-group&group=SENTINEL_GROUP")
if echo "$API_RESULT" | grep -q "consumer_api"; then
  echo "  ✓ gw-api-group 配置已写入"
else
  echo "  ✗ gw-api-group 配置写入失败"
  exit 1
fi

FLOW_RESULT=$(curl -s "$NACOS_CONFIG_URL/getConfig?dataId=cloud.sample.gateway.gw-flow&group=SENTINEL_GROUP")
if echo "$FLOW_RESULT" | grep -q "consumer_api"; then
  echo "  ✓ gw-flow 配置已写入"
else
  echo "  ✗ gw-flow 配置写入失败"
  exit 1
fi

# 等待 Gateway 加载规则
echo "  等待 Gateway 加载规则..."
sleep 3

# ========== Step 4: 触发限流验证 ==========
echo ""
echo "=========================================="
echo "  触发限流验证 (consumer_api 阈值: 5 QPS)"
echo "=========================================="

BLOCKED=false
PASS_COUNT=0
BLOCK_COUNT=0

for i in $(seq 1 10); do
  RESP=$(curl -s "$GATEWAY_URL/consumer-sample/hi?name=hongxi")
  if echo "$RESP" | grep -q "444"; then
    BLOCK_COUNT=$((BLOCK_COUNT + 1))
    STATUS="BLOCKED"
    BLOCKED=true
  else
    PASS_COUNT=$((PASS_COUNT + 1))
    STATUS="PASS"
  fi
  echo "  请求 $i: [$STATUS] $RESP"
done

echo ""
echo "=========================================="
echo "  结果: 通过=$PASS_COUNT, 拦截=$BLOCK_COUNT"
echo "=========================================="

if [ "$BLOCKED" = true ] && [ $BLOCK_COUNT -gt 0 ]; then
  echo "  ✓ Sentinel 网关限流生效"
else
  echo "  ✗ 未触发限流，请检查配置"
fi

# ========== Step 5: 清理 Sentinel 配置 ==========
echo ""
echo ">>> Step 5: 清理 Sentinel 配置..."

curl -s "$NACOS_CONFIG_URL/removeConfig?dataId=cloud.sample.gateway.gw-api-group&group=SENTINEL_GROUP" > /dev/null
curl -s "$NACOS_CONFIG_URL/removeConfig?dataId=cloud.sample.gateway.gw-flow&group=SENTINEL_GROUP" > /dev/null
echo "  ✓ 已清理 Sentinel 配置"

echo ""
echo "=========================================="
echo "  验证完成！"
echo "=========================================="
