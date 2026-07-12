#!/bin/bash
# Kafka 4.x KRaft 3 节点集群一键管理脚本（Mac/Linux）
# 用法:
#   bash kafka.sh start   — 检测/解压 Kafka，创建配置，格式化，启动 3 节点集群
#   bash kafka.sh stop    — 停止 3 节点集群
#   bash kafka.sh status  — 检查集群状态
#   bash kafka.sh restart — 停止后重新启动
#   bash kafka.sh clean   — 停止集群并清理存储（下次 start 会重新格式化）
#
# 集群端口规划:
#   Broker 1: PLAINTEXT=9092, CONTROLLER=9093
#   Broker 2: PLAINTEXT=9094, CONTROLLER=9095
#   Broker 3: PLAINTEXT=9096, CONTROLLER=9097
#
# Kafka 4.x 需在 broker 配置 share.group.protocol=share 以启用 Share Groups (KIP-932)

set -e

# ========== 颜色与工具函数 ==========
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${GREEN}✓${NC} $1"; }
warn()  { echo -e "${YELLOW}⚠${NC} $1"; }
err()   { echo -e "${RED}✗${NC} $1"; }
step()  { echo -e "\n${CYAN}>>> $1${NC}"; }

# ========== 查找 Kafka 安装目录 ==========
find_kafka_home() {
  local kafka_dir=""

  # 优先级 1: $HOME 下的 kafka_* 目录（取最新版本）
  kafka_dir=$(find "$HOME" -maxdepth 1 -type d -name 'kafka_*' 2>/dev/null | sort -V | tail -1)
  if [ -n "$kafka_dir" ] && [ -f "$kafka_dir/bin/kafka-server-start.sh" ]; then
    echo "$kafka_dir"
    return 0
  fi

  # 优先级 2: $HOME/Downloads 下的 kafka_* 目录
  if [ -d "$HOME/Downloads" ]; then
    kafka_dir=$(find "$HOME/Downloads" -maxdepth 1 -type d -name 'kafka_*' 2>/dev/null | sort -V | tail -1)
    if [ -n "$kafka_dir" ] && [ -f "$kafka_dir/bin/kafka-server-start.sh" ]; then
      echo "$kafka_dir"
      return 0
    fi
  fi

  # 优先级 3: $HOME 下的 kafka_*.tgz 压缩包
  local tgz=""
  tgz=$(find "$HOME" -maxdepth 1 -type f -name 'kafka_*.tgz' 2>/dev/null | sort -V | tail -1)
  if [ -z "$tgz" ] && [ -d "$HOME/Downloads" ]; then
    tgz=$(find "$HOME/Downloads" -maxdepth 1 -type f -name 'kafka_*.tgz' 2>/dev/null | sort -V | tail -1)
  fi
  if [ -n "$tgz" ]; then
    info "找到 Kafka 压缩包: $tgz"
    local extract_dir
    extract_dir=$(dirname "$tgz")
    echo "  解压中..."
    tar -xzf "$tgz" -C "$extract_dir"
    # 解压后重新查找
    kafka_dir=$(find "$extract_dir" -maxdepth 1 -type d -name 'kafka_*' 2>/dev/null | sort -V | tail -1)
    if [ -n "$kafka_dir" ] && [ -f "$kafka_dir/bin/kafka-server-start.sh" ]; then
      echo "$kafka_dir"
      return 0
    fi
  fi

  return 1
}

# ========== 检查集群是否已运行 ==========
check_brokers() {
  local ready=0
  nc -z 127.0.0.1 9092 2>/dev/null && ready=$((ready + 1))
  nc -z 127.0.0.1 9094 2>/dev/null && ready=$((ready + 1))
  nc -z 127.0.0.1 9096 2>/dev/null && ready=$((ready + 1))
  echo "$ready"
}

# ========== 创建 3 份 KRaft 配置文件 ==========
create_configs() {
  local kafka_home="$1"
  local config_dir="$kafka_home/config"

  info "创建 3 节点 KRaft 配置文件..."

  cat > "$config_dir/server-1.properties" << 'CONF'
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@localhost:9093,2@localhost:9095,3@localhost:9097
controller.listener.names=CONTROLLER
listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093
advertised.listeners=PLAINTEXT://localhost:9092
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
log.dirs=/tmp/kraft-logs-1
auto.create.topics.enable=true
share.group.protocol=share
CONF

  cat > "$config_dir/server-2.properties" << 'CONF'
process.roles=broker,controller
node.id=2
controller.quorum.voters=1@localhost:9093,2@localhost:9095,3@localhost:9097
controller.listener.names=CONTROLLER
listeners=PLAINTEXT://localhost:9094,CONTROLLER://localhost:9095
advertised.listeners=PLAINTEXT://localhost:9094
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
log.dirs=/tmp/kraft-logs-2
auto.create.topics.enable=true
share.group.protocol=share
CONF

  cat > "$config_dir/server-3.properties" << 'CONF'
process.roles=broker,controller
node.id=3
controller.quorum.voters=1@localhost:9093,2@localhost:9095,3@localhost:9097
controller.listener.names=CONTROLLER
listeners=PLAINTEXT://localhost:9096,CONTROLLER://localhost:9097
advertised.listeners=PLAINTEXT://localhost:9096
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
log.dirs=/tmp/kraft-logs-3
auto.create.topics.enable=true
share.group.protocol=share
CONF

  info "server-1.properties / server-2.properties / server-3.properties 已创建"
}

# ========== 格式化 KRaft 存储 ==========
format_storage() {
  local kafka_home="$1"
  local cluster_id
  cluster_id=$("$kafka_home/bin/kafka-storage.sh" random-uuid)
  info "生成 Cluster ID: $cluster_id"

  echo "  格式化节点 1..."
  "$kafka_home/bin/kafka-storage.sh" format -t "$cluster_id" -c "$kafka_home/config/server-1.properties" 2>&1 | grep -v "^Bootstrap\|^Formatting" | sed 's/^/  /'
  echo "  格式化节点 2..."
  "$kafka_home/bin/kafka-storage.sh" format -t "$cluster_id" -c "$kafka_home/config/server-2.properties" 2>&1 | grep -v "^Bootstrap\|^Formatting" | sed 's/^/  /'
  echo "  格式化节点 3..."
  "$kafka_home/bin/kafka-storage.sh" format -t "$cluster_id" -c "$kafka_home/config/server-3.properties" 2>&1 | grep -v "^Bootstrap\|^Formatting" | sed 's/^/  /'

  info "3 个节点 KRaft 存储格式化完成"
}

# ========== 清理存储 ==========
clean_storage() {
  rm -rf /tmp/kraft-logs-1 /tmp/kraft-logs-2 /tmp/kraft-logs-3
  info "已清理 /tmp/kraft-logs-{1,2,3}"
}

# ========== 启动集群 ==========
do_start() {
  step "Step 1: 查找 Kafka 安装..."
  local kafka_home
  kafka_home=$(find_kafka_home)
  if [ -z "$kafka_home" ]; then
    err "未找到 Kafka 4.x 安装包"
    echo "  请将 kafka_2.13-4.x.x.tgz 下载到 $HOME 或 $HOME/Downloads 目录"
    exit 1
  fi
  info "Kafka 安装目录: $kafka_home"

  # 检查是否已有集群在运行
  local running
  running=$(check_brokers)
  if [ "$running" -eq 3 ]; then
    info "Kafka 3 节点集群已在运行 (9092/9094/9096)"
    return 0
  elif [ "$running" -gt 0 ]; then
    warn "检测到 $running/3 个 Broker 端口可达，集群不完整，尝试重新启动..."
    "$kafka_home/bin/kafka-server-stop.sh" 2>/dev/null || true
    sleep 3
  fi

  # 检查配置文件
  step "Step 2: 检查 KRaft 配置文件..."
  local config_ok=true
  for i in 1 2 3; do
    if [ ! -f "$kafka_home/config/server-$i.properties" ]; then
      config_ok=false
      break
    fi
  done

  if [ "$config_ok" = true ]; then
    info "config/server-{1,2,3}.properties 已存在，跳过创建"
  else
    create_configs "$kafka_home"
  fi

  # 检查存储是否需要格式化
  step "Step 3: 格式化 KRaft 存储..."
  local need_format=false
  for i in 1 2 3; do
    if [ ! -d "/tmp/kraft-logs-$i" ] || [ -z "$(ls -A /tmp/kraft-logs-$i 2>/dev/null)" ]; then
      need_format=true
      break
    fi
  done

  if [ "$need_format" = true ]; then
    format_storage "$kafka_home"
  else
    info "KRaft 存储目录已存在，跳过格式化（如需重新格式化请使用 clean 命令）"
  fi

  # 启动 3 个 Broker
  step "Step 4: 启动 3 节点 KRaft 集群..."
  echo "  启动 Broker 1 (端口 9092/9093)..."
  "$kafka_home/bin/kafka-server-start.sh" -daemon "$kafka_home/config/server-1.properties"
  sleep 2
  echo "  启动 Broker 2 (端口 9094/9095)..."
  "$kafka_home/bin/kafka-server-start.sh" -daemon "$kafka_home/config/server-2.properties"
  sleep 2
  echo "  启动 Broker 3 (端口 9096/9097)..."
  "$kafka_home/bin/kafka-server-start.sh" -daemon "$kafka_home/config/server-3.properties"

  # 等待集群就绪
  step "Step 5: 等待集群就绪..."
  local waited=0
  while [ $waited -lt 60 ]; do
    running=$(check_brokers)
    if [ "$running" -eq 3 ]; then
      echo ""
      info "Kafka 3 节点集群启动成功! (${waited}s)"
      echo ""
      echo "  Broker 1: localhost:9092 (Controller: 9093)"
      echo "  Broker 2: localhost:9094 (Controller: 9095)"
      echo "  Broker 3: localhost:9096 (Controller: 9097)"
      echo ""
      info "Kafka 4.x 已启用 Share Groups (KIP-932, share.group.protocol=share) 和事务消息"
      return 0
    fi
    printf "\r  等待中... %ds (已就绪 %s/3)" "$waited" "$running"
    sleep 2
    waited=$((waited + 2))
  done

  echo ""
  err "集群启动超时，请检查日志: $kafka_home/logs/"
  echo "  常见原因: 元数据损坏 → 执行 'bash kafka.sh clean' 后重新 start"
  exit 1
}

# ========== 停止集群 ==========
do_stop() {
  step "停止 Kafka 集群..."
  local kafka_home
  kafka_home=$(find_kafka_home)
  if [ -z "$kafka_home" ]; then
    err "未找到 Kafka 安装目录"
    exit 1
  fi

  local running
  running=$(check_brokers)
  if [ "$running" -eq 0 ]; then
    info "Kafka 集群未在运行"
    return 0
  fi

  "$kafka_home/bin/kafka-server-stop.sh" 2>/dev/null || true
  echo "  等待进程退出..."
  local waited=0
  while [ $waited -lt 30 ]; do
    running=$(check_brokers)
    if [ "$running" -eq 0 ]; then
      info "Kafka 集群已停止"
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done
  warn "部分 Broker 可能未完全退出，请稍后检查"
}

# ========== 集群状态 ==========
do_status() {
  echo "=========================================="
  echo "  Kafka 4.x KRaft 集群状态"
  echo "=========================================="
  echo ""

  local kafka_home
  kafka_home=$(find_kafka_home) || true
  if [ -n "$kafka_home" ]; then
    info "安装目录: $kafka_home"
    local version
    version=$(ls "$kafka_home"/libs/kafka-server-common-*.jar 2>/dev/null | head -1 | grep -o '[0-9]\+\.[0-9]\+\.[0-9]\+' | head -1)
    [ -n "$version" ] && info "Kafka 版本: $version"
  else
    warn "未找到 Kafka 安装"
  fi

  echo ""
  echo "  Broker 状态:"
  nc -z 127.0.0.1 9092 2>/dev/null && echo -e "    ${GREEN}✓${NC} Broker 1: localhost:9092 (Controller: 9093)" || echo -e "    ${RED}✗${NC} Broker 1: localhost:9092 (Controller: 9093)"
  nc -z 127.0.0.1 9094 2>/dev/null && echo -e "    ${GREEN}✓${NC} Broker 2: localhost:9094 (Controller: 9095)" || echo -e "    ${RED}✗${NC} Broker 2: localhost:9094 (Controller: 9095)"
  nc -z 127.0.0.1 9096 2>/dev/null && echo -e "    ${GREEN}✓${NC} Broker 3: localhost:9096 (Controller: 9097)" || echo -e "    ${RED}✗${NC} Broker 3: localhost:9096 (Controller: 9097)"

  local running
  running=$(check_brokers)
  echo ""
  if [ "$running" -eq 3 ]; then
    info "集群状态: 正常运行 (3/3)"
  elif [ "$running" -gt 0 ]; then
    warn "集群状态: 部分运行 ($running/3)"
  else
    warn "集群状态: 未运行"
  fi

  # 检查 KRaft 存储
  echo ""
  echo "  存储目录:"
  for i in 1 2 3; do
    if [ -d "/tmp/kraft-logs-$i" ] && [ -n "$(ls -A /tmp/kraft-logs-$i 2>/dev/null)" ]; then
      echo -e "    ${GREEN}✓${NC} /tmp/kraft-logs-$i"
    else
      echo -e "    ${RED}✗${NC} /tmp/kraft-logs-$i (未格式化)"
    fi
  done
  echo ""
}

# ========== 查看 Topic ==========
do_topics() {
  local kafka_home
  kafka_home=$(find_kafka_home)
  if [ -z "$kafka_home" ]; then
    err "未找到 Kafka 安装目录"
    exit 1
  fi

  local running
  running=$(check_brokers)
  if [ "$running" -eq 0 ]; then
    err "Kafka 集群未运行，请先执行 start"
    exit 1
  fi

  # 找到可用的 broker 端口
  local bootstrap=""
  if nc -z 127.0.0.1 9092 2>/dev/null; then
    bootstrap="localhost:9092"
  fi
  if nc -z 127.0.0.1 9094 2>/dev/null; then
    [ -n "$bootstrap" ] && bootstrap="$bootstrap,"
    bootstrap="${bootstrap}localhost:9094"
  fi
  if nc -z 127.0.0.1 9096 2>/dev/null; then
    [ -n "$bootstrap" ] && bootstrap="$bootstrap,"
    bootstrap="${bootstrap}localhost:9096"
  fi

  local describe="${2:-}"

  if [ "$describe" = "-d" ] || [ "$describe" = "--describe" ]; then
    # 详细模式：列出每个 topic 的分区/副本信息
    local topics
    topics=$("$kafka_home/bin/kafka-topics.sh" --bootstrap-server "$bootstrap" --list 2>/dev/null)
    if [ -z "$topics" ]; then
      warn "当前集群无任何 Topic"
      return
    fi
    echo "=========================================="
    echo "  Kafka Topic 详情 [$bootstrap]"
    echo "=========================================="
    while IFS= read -r topic; do
      echo ""
      "$kafka_home/bin/kafka-topics.sh" --bootstrap-server "$bootstrap" --describe --topic "$topic" 2>/dev/null
    done <<< "$topics"
    echo ""
  else
    # 简洁模式：仅列出 topic 名称
    echo "=========================================="
    echo "  Kafka Topic 列表 [$bootstrap]"
    echo "=========================================="
    local topics
    topics=$("$kafka_home/bin/kafka-topics.sh" --bootstrap-server "$bootstrap" --list 2>/dev/null)
    if [ -z "$topics" ]; then
      echo ""
      warn "当前集群无任何 Topic"
      echo ""
      return
    fi
    echo ""
    local count=0
    while IFS= read -r topic; do
      count=$((count + 1))
      printf "  %d. %s\n" "$count" "$topic"
    done <<< "$topics"
    echo ""
    info "共 $count 个 Topic（查看详情: bash kafka.sh topics -d）"
    echo ""
  fi
}

# ========== 清理并重置 ==========
do_clean() {
  do_stop
  clean_storage
  info "集群已停止并清理，下次执行 start 将重新格式化"
}

# ========== 主入口 ==========
case "${1:-start}" in
  start)   do_start ;;
  stop)    do_stop ;;
  status)  do_status ;;
  restart) do_stop; sleep 2; do_start ;;
  topics)  do_topics "$@" ;;
  clean)   do_clean ;;
  *)
    echo "用法: bash $0 {start|stop|status|restart|topics|clean}"
    echo ""
    echo "  start          — 检测/解压 Kafka，创建配置，格式化，启动集群"
    echo "  stop           — 停止集群"
    echo "  status         — 查看集群状态"
    echo "  restart        — 重启集群"
    echo "  topics [-d]    — 列出所有 Topic（-d 显示分区/副本详情）"
    echo "  clean          — 停止并清理存储（下次 start 重新格式化）"
    exit 1
    ;;
esac
