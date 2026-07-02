#!/bin/bash
# Sentinel 应用级熔断降级验证脚本
# 用法: bash .qoder/skills/demo-spring-cloud/verify-sentinel-app.sh
# 自动启动: cloud-nacos-config-sample(8761), cloud-provider-sample(8765),
#           cloud-consumer-sample(8766)

cd "$(dirname "$0")/../../.."
PROJECT_DIR=$(pwd)
mkdir -p logs .pids

NACOS_CONFIG_URL="http://localhost:8761/nacos"
CONSUMER_URL="http://localhost:8766"
PROVIDER_URL="http://localhost:8765"

echo "=========================================="
echo "  Sentinel 应用级熔断降级验证"
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

# 1b: 检查/启动 nacos-config-sample (8761)
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8761/actuator/health" 2>/dev/null | grep -q "200"; then
  echo "  ✓ NacosConfig (8761) 已运行"
else
  echo "  → NacosConfig (8761) 未运行，正在启动..."
  if [ ! -f cloud-nacos-config-sample/target/cloud-nacos-config-sample.jar ]; then
    echo "  打包 cloud-nacos-config-sample..."
    ./mvnw -pl cloud-nacos-config-sample -am package -DskipTests -q
  fi
  java -jar cloud-nacos-config-sample/target/cloud-nacos-config-sample.jar > logs/nacos-config.log 2>&1 &
  echo $! > .pids/nacos-config.pid
  echo "  NacosConfig 启动中 (PID: $!)..."
  for i in $(seq 1 60); do
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8761/actuator/health" 2>/dev/null | grep -q "200"; then
      echo "  ✓ NacosConfig (8761) 已就绪 (${i}s)"
      break
    fi
    if [ $i -eq 60 ]; then
      echo "  ✗ NacosConfig 启动超时，请查看 logs/nacos-config.log"
      exit 1
    fi
    sleep 1
  done
fi

# 1c: 检查/启动 provider-sample (8765)
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8765/actuator/health" 2>/dev/null | grep -q "200"; then
  echo "  ✓ Provider (8765) 已运行"
else
  echo "  → Provider (8765) 未运行，正在启动..."
  if [ ! -f cloud-provider-sample/target/cloud-provider-sample.jar ]; then
    echo "  打包 cloud-provider-sample..."
    ./mvnw -pl cloud-provider-sample -am package -DskipTests -q
  fi
  java -jar cloud-provider-sample/target/cloud-provider-sample.jar > logs/provider.log 2>&1 &
  echo $! > .pids/provider.pid
  echo "  Provider 启动中 (PID: $!)..."
  for i in $(seq 1 60); do
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8765/actuator/health" 2>/dev/null | grep -q "200"; then
      echo "  ✓ Provider (8765) 已就绪 (${i}s)"
      break
    fi
    if [ $i -eq 60 ]; then
      echo "  ✗ Provider 启动超时，请查看 logs/provider.log"
      exit 1
    fi
    sleep 1
  done
fi

# 1d: 检查/启动 consumer-sample (8766)
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8766/actuator/health" 2>/dev/null | grep -q "200"; then
  echo "  ✓ Consumer (8766) 已运行"
else
  echo "  → Consumer (8766) 未运行，正在启动..."
  if [ ! -f cloud-consumer-sample/target/cloud-consumer-sample.jar ]; then
    echo "  打包 cloud-consumer-sample..."
    ./mvnw -pl cloud-consumer-sample -am package -DskipTests -q
  fi
  java -jar cloud-consumer-sample/target/cloud-consumer-sample.jar > logs/consumer.log 2>&1 &
  echo $! > .pids/consumer.pid
  echo "  Consumer 启动中 (PID: $!)..."
  for i in $(seq 1 60); do
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8766/actuator/health" 2>/dev/null | grep -q "200"; then
      echo "  ✓ Consumer (8766) 已就绪 (${i}s)"
      break
    fi
    if [ $i -eq 60 ]; then
      echo "  ✗ Consumer 启动超时，请查看 logs/consumer.log"
      exit 1
    fi
    sleep 1
  done
fi

# ========== Step 2: 基线验证 ==========
echo ""
echo ">>> Step 2: 基线验证（正常调用）..."

BASELINE=$(curl -s --max-time 5 "$CONSUMER_URL/hi?name=test&version=2.0")
if echo "$BASELINE" | grep -q "Hi, test"; then
  echo "  ✓ Feign 调用正常: $BASELINE"
else
  echo "  ✗ Feign 调用异常: $BASELINE"
  exit 1
fi

# ========== Step 3: 清理旧规则 ==========
echo ""
echo ">>> Step 3: 清理旧规则..."

curl -s --get --data-urlencode "dataId=cloud.sample.consumer.flow" --data-urlencode "group=SENTINEL_GROUP" "$NACOS_CONFIG_URL/removeConfig" > /dev/null
curl -s --get --data-urlencode "dataId=cloud.sample.consumer.degrade" --data-urlencode "group=SENTINEL_GROUP" "$NACOS_CONFIG_URL/removeConfig" > /dev/null
echo "  ✓ 已清理旧规则"
sleep 1

# ========== Step 4: 限流验证（consumer 自身接口） ==========
echo ""
echo "=========================================="
echo "  限流验证 (资源名: /hi, QPS=1)"
echo "=========================================="

# 推送限流规则
FLOW_JSON='[{"resource":"/hi","grade":1,"count":1}]'
curl -s --get --data-urlencode "dataId=cloud.sample.consumer.flow" \
  --data-urlencode "group=SENTINEL_GROUP" \
  --data-urlencode "type=json" \
  --data-urlencode "content=$FLOW_JSON" "$NACOS_CONFIG_URL/publishConfig" > /dev/null
echo "  ✓ 已推送限流规则"
sleep 1

# 快速连续请求
echo ""
echo "  快速连续请求:"
PASS_COUNT=0
BLOCK_COUNT=0

for i in 1 2; do
  RESP=$(curl -s --max-time 3 "$CONSUMER_URL/hi?name=test&version=2.0")
  if echo "$RESP" | grep -q "Blocked"; then
    BLOCK_COUNT=$((BLOCK_COUNT + 1))
    echo "    请求 $i: [BLOCKED] $RESP"
  else
    PASS_COUNT=$((PASS_COUNT + 1))
    echo "    请求 $i: [PASS] $RESP"
  fi
done

echo ""
if [ $BLOCK_COUNT -gt 0 ]; then
  echo "  ✓ Sentinel 接口限流生效 (通过=$PASS_COUNT, 拦截=$BLOCK_COUNT)"
else
  echo "  ✗ 未触发限流，请检查配置"
fi

# 清理限流规则
curl -s --get --data-urlencode "dataId=cloud.sample.consumer.flow" --data-urlencode "group=SENTINEL_GROUP" "$NACOS_CONFIG_URL/removeConfig" > /dev/null
sleep 1

# ========== Step 5: 熔断降级验证（Feign 出站调用） ==========
echo ""
echo "=========================================="
echo "  熔断降级验证 (资源名: GET:http://provider-sample/hello)"
echo "=========================================="

# 推送降级规则（异常比例策略，阈值 50%，熔断 10 秒）
# 注意: grade=2 时 count 为异常比例阈值，判断条件为 currentRatio > count
# count=1.0 意味着需要 >100% 异常才能熔断（不可能），所以用 0.5 表示 50%
# SentinelProtectInterceptor 创建两个资源:
#   1. hostResource:        GET:http://provider-sample   (不含路径，urlCleaner 去端口后)
#   2. hostWithPathResource: GET:http://provider-sample/hello   (含路径，urlCleaner 去端口后)
# 注意: urlCleaner 会去除端口号，所以规则中的资源名不应包含端口
DEGRADE_JSON='[{"resource":"GET:http://provider-sample/hello","grade":2,"count":0.5,"timeWindow":10,"minRequestAmount":1,"statIntervalMs":10000},{"resource":"GET:http://provider-sample","grade":2,"count":0.5,"timeWindow":10,"minRequestAmount":1,"statIntervalMs":10000}]'
curl -s --get --data-urlencode "dataId=cloud.sample.consumer.degrade" \
  --data-urlencode "group=SENTINEL_GROUP" \
  --data-urlencode "type=json" \
  --data-urlencode "content=$DEGRADE_JSON" "$NACOS_CONFIG_URL/publishConfig" > /dev/null
echo "  ✓ 已推送降级规则"
sleep 1

# 停止 provider
echo ""
echo "  停止 provider-sample..."
PROVIDER_PID=$(lsof -ti :8765 -sTCP:LISTEN 2>/dev/null)
if [ -n "$PROVIDER_PID" ]; then
  kill -9 $PROVIDER_PID 2>/dev/null
  echo "  ✓ provider-sample 已停止 (PID: $PROVIDER_PID)"
  sleep 2
else
  echo "  ✗ provider-sample 未在运行"
fi

# 触发熔断降级 - Feign 路径
echo ""
echo "  触发熔断降级 (Feign, version=2.0):"
FEIGN_RESP=$(curl -s --max-time 5 "$CONSUMER_URL/hi?name=test&version=2.0")
if echo "$FEIGN_RESP" | grep -q "fallback"; then
  echo "    ✓ Feign fallback: $FEIGN_RESP"
else
  echo "    ✗ 未触发 Feign fallback: $FEIGN_RESP"
fi

# 触发熔断降级 - RestTemplate 路径
echo ""
echo "  触发熔断降级 (RestTemplate, version=1.0):"
# 第一次请求触发熔断（provider 已停止，异常被 Sentinel 记录，熔断器打开）
echo "  预热请求（触发熔断器打开）..."
curl -s --max-time 5 "$CONSUMER_URL/hi?name=test&version=1.0" > /dev/null
sleep 1
# 第二次请求应被 Sentinel 熔断拦截，触发 fallback
RT_RESP=$(curl -s --max-time 5 "$CONSUMER_URL/hi?name=test&version=1.0")
if echo "$RT_RESP" | grep -q "Blocked by Sentinel"; then
  echo "    ✓ RestTemplate fallback: $RT_RESP"
else
  echo "    ✗ 未触发 RestTemplate fallback: $RT_RESP"
fi

# ========== Step 6: 恢复环境 ==========
echo ""
echo ">>> Step 6: 恢复环境..."

# 清理降级规则
curl -s --get --data-urlencode "dataId=cloud.sample.consumer.degrade" --data-urlencode "group=SENTINEL_GROUP" "$NACOS_CONFIG_URL/removeConfig" > /dev/null
echo "  ✓ 已清理降级规则"

# 重启 provider
echo "  重启 provider-sample..."
cd "$(dirname "$0")/../../.."
java -jar cloud-provider-sample/target/cloud-provider-sample.jar > logs/provider.log 2>&1 &
echo $! > .pids/provider.pid
echo "  ✓ provider-sample 启动中 (PID: $(cat .pids/provider.pid))..."

# 等待 provider 就绪
for i in $(seq 1 30); do
  if curl -s --max-time 2 "$PROVIDER_URL/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    echo "  ✓ provider-sample 已就绪 (${i}s)"
    break
  fi
  if [ $i -eq 30 ]; then
    echo "  ✗ provider-sample 启动超时"
  fi
  sleep 1
done

# ========== 汇总 ==========
echo ""
echo "=========================================="
echo "  验证完成！"
echo "=========================================="
echo ""
echo "  限流: 资源名 /hi (URI 自动注册)，QPS=1 时触发拦截"
echo "  熔断: 资源名 GET:http://provider-sample (urlCleaner 去端口后)"
echo "        Feign (v2.0) → ProviderClientFallback"
echo "        RestTemplate (v1.0) → SentinelExceptionHandler.handleFallback"
echo ""
