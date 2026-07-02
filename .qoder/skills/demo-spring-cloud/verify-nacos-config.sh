#!/bin/bash
# Nacos Config 动态配置验证脚本
# 用法: bash .qoder/skills/demo-spring-cloud/verify-nacos-config.sh
# 自动启动: cloud-nacos-config-sample(8761)

cd "$(dirname "$0")/../../.."
PROJECT_DIR=$(pwd)
mkdir -p logs .pids

NACOS_CONFIG_URL="http://localhost:8761/nacos"
APP_URL="http://localhost:8761"

VERIFY_PASS=0
VERIFY_FAIL=0

pass() { VERIFY_PASS=$((VERIFY_PASS + 1)); echo "  ✓ $1"; }
fail() { VERIFY_FAIL=$((VERIFY_FAIL + 1)); echo "  ✗ $1"; }

echo "=========================================="
echo "  Nacos Config 动态配置验证"
echo "=========================================="

# ========== Step 1: 检查/启动 nacos-config 模块 ==========
echo ""
echo ">>> Step 1: 检查/启动 nacos-config 模块..."

# 检查 Nacos
if curl -s -o /dev/null -w '' "http://127.0.0.1:8848/nacos/actuator/health" 2>/dev/null; then
  echo "  ✓ Nacos 已运行"
else
  echo "  ✗ Nacos 未运行，请先启动 Nacos"
  exit 1
fi

if curl -s -o /dev/null -w "%{http_code}" "$APP_URL/actuator/health" 2>/dev/null | grep -q "200"; then
  pass "nacos-config (8761) 已运行"
else
  echo "  → nacos-config (8761) 未运行，正在启动..."
  if [ ! -f cloud-nacos-config-sample/target/cloud-nacos-config-sample.jar ]; then
    echo "  打包 cloud-nacos-config-sample..."
    ./mvnw -pl cloud-nacos-config-sample -am package -DskipTests -q
  fi
  java -jar cloud-nacos-config-sample/target/cloud-nacos-config-sample.jar > logs/nacos-config.log 2>&1 &
  echo $! > .pids/nacos-config.pid
  echo "  nacos-config 启动中 (PID: $!)..."
  for i in $(seq 1 60); do
    if curl -s -o /dev/null -w "%{http_code}" "$APP_URL/actuator/health" 2>/dev/null | grep -q "200"; then
      pass "nacos-config (8761) 已就绪 (${i}s)"
      break
    fi
    if [ $i -eq 60 ]; then
      fail "nacos-config 启动超时，请查看 logs/nacos-config.log"
      exit 1
    fi
    sleep 1
  done
fi

# ========== Step 2: 基础配置管理 (发布/读取/删除) ==========
echo ""
echo ">>> Step 2: 基础配置管理 (publishConfig / getConfig / removeConfig)..."

# 发布配置
PUBLISH_RESULT=$(curl -s "$NACOS_CONFIG_URL/publishConfig?dataId=my.city&content=wuhan")
if [ "$PUBLISH_RESULT" = "true" ]; then
  pass "发布配置 my.city=wuhan"
else
  fail "发布配置 my.city 失败: $PUBLISH_RESULT"
fi

sleep 1

# 读取配置
GET_RESULT=$(curl -s "$NACOS_CONFIG_URL/getConfig?dataId=my.city")
if echo "$GET_RESULT" | grep -q "wuhan"; then
  pass "读取配置 my.city=$GET_RESULT"
else
  fail "读取配置 my.city 失败: $GET_RESULT"
fi

# 删除配置
REMOVE_RESULT=$(curl -s "$NACOS_CONFIG_URL/removeConfig?dataId=my.city")
if [ "$REMOVE_RESULT" = "true" ]; then
  pass "删除配置 my.city"
else
  fail "删除配置 my.city 失败: $REMOVE_RESULT"
fi

sleep 1

# 确认删除
GET_AFTER_REMOVE=$(curl -s "$NACOS_CONFIG_URL/getConfig?dataId=my.city")
if [ -z "$GET_AFTER_REMOVE" ]; then
  pass "确认配置已删除"
else
  fail "配置未删除: $GET_AFTER_REMOVE"
fi

# ========== Step 3: @NacosConfig 注解验证 ==========
echo ""
echo ">>> Step 3: @NacosConfig 注解 (github.username → /config/hello)..."

# 发布配置
curl -s "$NACOS_CONFIG_URL/publishConfig?dataId=github.username&content=javahongxi" > /dev/null
sleep 2

# 验证注入
HELLO_RESULT=$(curl -s "$APP_URL/config/hello")
if echo "$HELLO_RESULT" | grep -q "javahongxi"; then
  pass "@NacosConfig 注入: $HELLO_RESULT"
else
  fail "@NacosConfig 注入失败: $HELLO_RESULT"
fi

# 修改配置，观察动态刷新
curl -s "$NACOS_CONFIG_URL/publishConfig?dataId=github.username&content=hongxi" > /dev/null
sleep 2

HELLO_RESULT2=$(curl -s "$APP_URL/config/hello")
if echo "$HELLO_RESULT2" | grep -q "hongxi"; then
  pass "@NacosConfig 动态刷新: $HELLO_RESULT2"
else
  fail "@NacosConfig 动态刷新失败: $HELLO_RESULT2"
fi

# 清理
curl -s "$NACOS_CONFIG_URL/removeConfig?dataId=github.username" > /dev/null

# ========== Step 4: @ConfigurationProperties + @Value 验证 ==========
echo ""
echo ">>> Step 4: @ConfigurationProperties + @Value (cloud-agent.properties)..."

# 发布 Properties 格式配置
CONTENT="cloud.agent.name=Trae CN
cloud.agent.version=3.3.60
cloud.agent.credits=2000000
cloud.agent.enabled=true
cloud.agent.provider.name=Alibaba
cloud.agent.provider.model=Qwen3.7 Plus
cloud.agent.provider.api-key=xxx123aa"

curl -s -X POST "$NACOS_CONFIG_URL/publishConfig" \
  --data-urlencode "dataId=cloud-agent.properties" \
  --data-urlencode "type=properties" \
  --data-urlencode "content=$CONTENT" > /dev/null
sleep 2

# 验证 @ConfigurationProperties Bean 绑定
AGENT_RESULT=$(curl -s "$APP_URL/config/agent")
if echo "$AGENT_RESULT" | grep -q "Trae CN"; then
  pass "@ConfigurationProperties 绑定: $AGENT_RESULT"
else
  fail "@ConfigurationProperties 绑定失败: $AGENT_RESULT"
fi

# 验证 @Value + @RefreshScope 注入
VALUE_RESULT=$(curl -s "$APP_URL/config/value")
if echo "$VALUE_RESULT" | grep -q "Trae CN"; then
  pass "@Value + @RefreshScope 注入: $VALUE_RESULT"
else
  fail "@Value + @RefreshScope 注入失败: $VALUE_RESULT"
fi

# 修改配置，观察动态刷新
CONTENT_V2="cloud.agent.name=DeepSeek
cloud.agent.version=4.0.0
cloud.agent.credits=9999999
cloud.agent.enabled=false
cloud.agent.provider.name=DeepSeek Inc
cloud.agent.provider.model=DeepSeek-V3
cloud.agent.provider.api-key=sk-xxx"

curl -s -X POST "$NACOS_CONFIG_URL/publishConfig" \
  --data-urlencode "dataId=cloud-agent.properties" \
  --data-urlencode "type=properties" \
  --data-urlencode "content=$CONTENT_V2" > /dev/null
sleep 2

AGENT_RESULT2=$(curl -s "$APP_URL/config/agent")
if echo "$AGENT_RESULT2" | grep -q "DeepSeek"; then
  pass "@ConfigurationProperties 动态刷新: $AGENT_RESULT2"
else
  fail "@ConfigurationProperties 动态刷新失败: $AGENT_RESULT2"
fi

VALUE_RESULT2=$(curl -s "$APP_URL/config/value")
if echo "$VALUE_RESULT2" | grep -q "DeepSeek"; then
  pass "@Value + @RefreshScope 动态刷新: $VALUE_RESULT2"
else
  fail "@Value + @RefreshScope 动态刷新失败: $VALUE_RESULT2"
fi

# ========== Step 5: 清理配置 ==========
echo ""
echo ">>> Step 5: 清理测试配置..."

curl -s "$NACOS_CONFIG_URL/removeConfig?dataId=cloud-agent.properties" > /dev/null
pass "已清理测试配置"

# ========== 汇总结果 ==========
echo ""
echo "=========================================="
echo "  验证结果: 通过 $VERIFY_PASS 项, 失败 $VERIFY_FAIL 项"
echo "=========================================="

if [ "$VERIFY_FAIL" -eq 0 ]; then
  echo "  ★ 全部验证通过! Nacos Config 动态配置功能正常 ★"
else
  echo "  存在失败项，请检查 nacos-config 模块和 Nacos Server 状态"
  exit 1
fi

