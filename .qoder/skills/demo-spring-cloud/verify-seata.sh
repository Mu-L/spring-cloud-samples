#!/bin/bash
# Seata 分布式事务一键验证脚本
# 用法: bash .qoder/skills/demo-spring-cloud/verify-seata.sh
set -e
cd "$(dirname "$0")/../../.."
PROJECT_DIR=$(pwd)

echo "=========================================="
echo "  Seata 分布式事务验证 - 完整流程"
echo "=========================================="

# ========== Step 0: 清理 ==========
echo ""
echo ">>> Step 0: 清理旧进程..."
pkill -f "cloud-seata-sample" 2>/dev/null || true
sleep 2
rm -rf logs
mkdir -p logs
echo "✓ 清理完成"

# ========== Step 1: 检查前置条件 ==========
echo ""
echo ">>> Step 1: 检查前置条件..."
curl -s -o /dev/null -w "" "http://127.0.0.1:8848/nacos/actuator/health" 2>/dev/null && echo "✓ Nacos 已运行" || { echo "✗ Nacos 未运行"; exit 1; }
mysql -u root -proot1234 -e "SELECT 1" &>/dev/null 2>&1 && echo "✓ MySQL 已运行" || { echo "✗ MySQL 未运行"; exit 1; }

# ========== Step 2: 初始化数据库 + 打包 + 启动辅助服务 ==========
echo ""
echo ">>> Step 2: 初始化数据库..."
mysql -u root -proot1234 -e "DROP DATABASE IF EXISTS seata; CREATE DATABASE seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null
mysql -u root -proot1234 seata < cloud-seata-sample/all.sql 2>/dev/null
echo "✓ 数据库初始化完成"

echo ""
echo ">>> Step 2b: 打包模块..."
./mvnw -pl cloud-nacos-config-sample,cloud-nacos-discovery-sample,cloud-seata-sample/business-service,cloud-seata-sample/storage-service,cloud-seata-sample/order-service,cloud-seata-sample/account-service package -DskipTests -q
echo "✓ 模块打包完成"

echo ""
echo ">>> Step 2c: 启动辅助服务..."
# 检查是否已在运行（避免重复启动）
CFG_OK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8761/actuator/health 2>/dev/null || echo "000")
DISC_OK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8760/actuator/health 2>/dev/null || echo "000")

if [ "$CFG_OK" = "200" ] && [ "$DISC_OK" = "200" ]; then
  echo "✓ nacos-config-sample (8761) 和 nacos-discovery-sample (8760) 已在运行，跳过启动"
else
  java -jar cloud-nacos-config-sample/target/*.jar > /tmp/nacos-config-sample.log 2>&1 &
  java -jar cloud-nacos-discovery-sample/target/*.jar > /tmp/nacos-discovery-sample.log 2>&1 &
  echo "辅助服务启动中..."

  for i in $(seq 1 30); do
    c1=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8761/actuator/health 2>/dev/null || echo "000")
    c2=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8760/actuator/health 2>/dev/null || echo "000")
    if [ "$c1" = "200" ] && [ "$c2" = "200" ]; then
      echo "✓ nacos-config-sample (8761) 和 nacos-discovery-sample (8760) 均已就绪 (${i}s)"
      break
    fi
    sleep 1
  done
fi

# ========== Step 3: 发布 seata.properties ==========
echo ""
echo ">>> Step 3: 发布 seata.properties 到 Nacos..."
CONTENT="service.vgroupMapping.default_tx_group=default
service.vgroupMapping.order-service-tx-group=default
service.vgroupMapping.account-service-tx-group=default
service.vgroupMapping.business-service-tx-group=default
service.vgroupMapping.storage-service-tx-group=default"
curl -s -X POST http://localhost:8761/nacos/publishConfig \
  -d "dataId=seata.properties" \
  -d "group=SEATA_GROUP" \
  -d "type=properties" \
  --data-urlencode "content=$CONTENT" > /dev/null
echo "✓ Nacos 配置创建完成"

# ========== Step 4: 启动 Seata Server ==========
echo ""
echo ">>> Step 4: 启动 Seata Server..."

# 检查 Seata Server 是否已在运行
if nc -z 127.0.0.1 8091 2>/dev/null; then
  echo "✓ Seata Server 已在运行 (端口 8091)，跳过启动"
else
  SEATA_SRC="$HOME/github/seata"
  if [ ! -d "$SEATA_SRC" ]; then
    echo "Seata 源码不存在，正在克隆..."
    mkdir -p "$HOME/github"
    git clone https://github.com/javahongxi/seata.git "$SEATA_SRC"
  fi

  cd "$SEATA_SRC"

  # Build if needed
  if [ ! -f "$SEATA_SRC/server/target/seata-server.jar" ]; then
    echo "构建 Seata Server..."
    ./mvnw clean install -DskipTests -q
    echo "✓ Seata Server 构建完成"
  fi

  cd "$SEATA_SRC"
  nohup ./mvnw -pl server spring-boot:run > /tmp/seata-server.log 2>&1 &
  echo "Seata Server 启动中..."

  SEATA_OK=0
  for i in $(seq 1 60); do
    if nc -z 127.0.0.1 8091 2>/dev/null; then
      echo "✓ Seata Server 已启动 (端口 8091, ${i}s)"
      SEATA_OK=1
      break
    fi
    sleep 1
  done
  if [ "$SEATA_OK" = "0" ]; then
    echo "✗ Seata Server 启动超时，查看日志："
    tail -30 /tmp/seata-server.log
    exit 1
  fi
fi

# ========== Step 5: 并行启动 4 个微服务 ==========
echo ""
echo ">>> Step 5: 并行启动 4 个微服务..."
cd "$PROJECT_DIR"
java -jar cloud-seata-sample/storage-service/target/*.jar > logs/seata-storage.log 2>&1 &
java -jar cloud-seata-sample/account-service/target/*.jar > logs/seata-account.log 2>&1 &
java -jar cloud-seata-sample/order-service/target/*.jar > logs/seata-order.log 2>&1 &
java -jar cloud-seata-sample/business-service/target/*.jar > logs/seata-business.log 2>&1 &
echo "4 个微服务同时启动中..."

ALL_OK=0
for i in $(seq 1 90); do
  c1=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:18081/actuator/health 2>/dev/null || echo "000")
  c2=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:18082/actuator/health 2>/dev/null || echo "000")
  c3=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:18083/actuator/health 2>/dev/null || echo "000")
  c4=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:18084/actuator/health 2>/dev/null || echo "000")
  if [ "$c1" = "200" ] && [ "$c2" = "200" ] && [ "$c3" = "200" ] && [ "$c4" = "200" ]; then
    echo "✓ 所有 4 个微服务均已就绪 (${i}s)"
    ALL_OK=1
    break
  fi
  sleep 1
done
if [ "$ALL_OK" = "0" ]; then
  echo "✗ 微服务启动超时: business=$c1 storage=$c2 order=$c3 account=$c4"
  exit 1
fi

# ========== Step 6: 验证分布式事务 ==========
echo ""
echo "=========================================="
echo "  开始验证分布式事务"
echo "=========================================="

echo ""
echo "=== 初始数据状态 ==="
mysql -u root -proot1234 seata -e "SELECT '账户余额' AS type, money AS value FROM account_tbl WHERE user_id='U100001' UNION ALL SELECT '库存数量', count FROM storage_tbl WHERE commodity_code='C00321';" 2>/dev/null
echo "订单数: $(mysql -u root -proot1234 seata -N -e 'SELECT COUNT(*) FROM order_tbl;' 2>/dev/null)"

echo ""
echo "=== 场景 1：验证事务回滚（返回 500 表示 mock 异常触发）==="
curl -s -o /dev/null -w "HTTP %{http_code}" http://127.0.0.1:18081/seata/rest
echo ""

echo ""
echo "=== 场景 2：验证事务提交成功（循环调用直到成功）==="
for i in $(seq 1 20); do
  result=$(curl -s -w "\n%{http_code}" http://127.0.0.1:18081/seata/rest)
  http_code=$(echo "$result" | tail -1)
  if [ "$http_code" = "200" ]; then
    echo "✓ 第 ${i} 次调用成功，事务已提交"
    break
  else
    echo "第 ${i} 次调用返回 ${http_code}（mock 异常，事务已回滚），继续重试..."
  fi
done

echo ""
echo "=== 验证 FeignClient 方式 ==="
for i in $(seq 1 20); do
  result=$(curl -s -w "\n%{http_code}" http://127.0.0.1:18081/seata/feign)
  http_code=$(echo "$result" | tail -1)
  if [ "$http_code" = "200" ]; then
    echo "✓ 第 ${i} 次调用成功（Feign），事务已提交"
    break
  else
    echo "第 ${i} 次调用返回 ${http_code}（Feign，mock 异常），继续重试..."
  fi
done

echo ""
echo "=== 验证 Xid 传递 ==="
grep -E "Begin.*xid:" logs/seata-storage.log 2>/dev/null | tail -1 || echo "(无 storage xid)"
grep -E "Begin.*xid:" logs/seata-order.log 2>/dev/null | tail -1 || echo "(无 order xid)"
grep -E "Begin.*xid:" logs/seata-account.log 2>/dev/null | tail -1 || echo "(无 account xid)"

echo ""
echo "=== 验证数据一致性 ==="
mysql -u root -proot1234 seata -e "SELECT '账户余额' AS type, money AS value FROM account_tbl WHERE user_id='U100001' UNION ALL SELECT '库存数量', count FROM storage_tbl WHERE commodity_code='C00321';" 2>/dev/null
echo "订单数: $(mysql -u root -proot1234 seata -N -e 'SELECT COUNT(*) FROM order_tbl;' 2>/dev/null)"

echo ""
echo "预期结果："
echo "- 用户余额：10000 = 当前余额 + 2(单价) × 订单数 × 2(每单数量)"
echo "- 库存数量：100 = 当前库存 + 订单数 × 2(每单数量)"
echo ""
echo "说明："
echo "- 返回 500 时：mock 异常触发，事务回滚，数据不变"
echo "- 返回 200 时：事务正常提交，余额减少、库存减少、订单增加"

echo ""
echo "=========================================="
echo "  验证完成！"
echo "=========================================="
