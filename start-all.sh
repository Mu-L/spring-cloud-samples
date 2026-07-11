#!/bin/bash
#
# 启动所有服务模块（除 cloud-sample-api cloud-commons 外）
# 使用方式: ./start-all.sh
# 停止所有服务: ./start-all.sh stop
#

set -e

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$BASE_DIR/logs"
PID_DIR="$BASE_DIR/.pids"

mkdir -p "$LOG_DIR" "$PID_DIR"

# 模块列表: 目录名 | 显示名称 | 端口（启动顺序与验证顺序一致）
MODULES=(
  "cloud-nacos-discovery-sample|nacos-discovery|8760"
  "cloud-nacos-config-sample|nacos-config|8761"
  "cloud-gateway-sample|gateway|8764"
  "cloud-provider-sample|provider|8765"
  "cloud-provider-reactive-sample|provider-reactive|8762"
  "cloud-provider-dubbo-sample|provider-dubbo|50051"
  "cloud-grpc-server-sample|grpc-server|8090"
  "cloud-consumer-sample|consumer|8766"
  "cloud-consumer-reactive-sample|consumer-reactive|8763"
)

# 特殊模块（需额外条件）
STREAM_MODULE=("cloud-stream-sample|stream|8767")
SEATA_MODULES=(
  "cloud-seata-sample/business-service|seata-business|18081"
  "cloud-seata-sample/storage-service|seata-storage|18082"
  "cloud-seata-sample/order-service|seata-order|18083"
  "cloud-seata-sample/account-service|seata-account|18084"
  "cloud-seata-sample/storage-dubbo-service|seata-storage-dubbo|50072"
  "cloud-seata-sample/order-dubbo-service|seata-order-dubbo|50073"
  "cloud-seata-sample/account-dubbo-service|seata-account-dubbo|50071"
)
AI_MODULE=("cloud-ai-sample|ai|8888")
RAG_MODULE=("cloud-ai-rag-sample|ai-rag|8889")
KAFKA_MODULE=("cloud-kafka-sample|kafka-sample|8768")

# 特殊模块启动标记
START_STREAM=false
START_SEATA=false
START_AI=false
START_RAG=false
START_KAFKA=false

check_java() {
  local min_version=17
  local java_version
  java_version=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+)\..*/\1/' 2>/dev/null || echo 0)

  if [ "$java_version" -ge "$min_version" ] 2>/dev/null; then
    echo "[Java] ✓ Java $java_version"
    return
  fi

  if ! command -v java &>/dev/null; then
    echo "[Java] ✗ 未检测到 Java"
  else
    echo "[Java] ✗ 当前 Java 版本: $java_version，需要 >= $min_version"
  fi
  printf '[Java] 是否自动安装 JDK %s? [Y/n] ' "$min_version"
  read -r answer
  if [[ "$answer" != "n" && "$answer" != "N" ]]; then
    if command -v brew &>/dev/null; then
      echo "[Java] 正在通过 Homebrew 安装 JDK $min_version ..."
      brew install openjdk@$min_version
      export JAVA_HOME="$(brew --prefix)/opt/openjdk@$min_version/libexec/openjdk.jdk/Contents/Home"
      export PATH="$JAVA_HOME/bin:$PATH"
      echo "[Java] ✓ JDK $min_version 已安装并激活（当前会话）"
      echo "[Java] 建议将以下内容添加到 ~/.zshrc 以永久生效:"
      echo "  export JAVA_HOME=$JAVA_HOME"
      echo "  export PATH=\$JAVA_HOME/bin:\$PATH"
    else
      echo "[Java] ✗ 未安装 Homebrew，请手动安装 JDK $min_version"
      return 1
    fi
  else
    echo "[Java] 跳过安装，继续执行..."
  fi
}

# Nacos 注册中心地址
NACOS_HOST="127.0.0.1"
NACOS_PORT="8848"

check_nacos() {
  printf '[Nacos] 检查注册中心 (%s:%s) ...' "$NACOS_HOST" "$NACOS_PORT"
  if curl -s -o /dev/null -w '' "http://$NACOS_HOST:$NACOS_PORT/nacos/actuator/health" 2>/dev/null; then
    echo " 就绪"
    return 0
  fi
  echo " 未运行，正在尝试自动启动..."
  local nacos_dir
  nacos_dir=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
  if [ -n "$nacos_dir" ] && [ -f "$nacos_dir/bin/startup.sh" ]; then
    bash "$nacos_dir/bin/startup.sh" -m standalone
    printf '[Nacos] 等待启动就绪...'
    wait_nacos_ready && return 0
  fi
  echo " 失败!"
  echo "请先安装并启动 Nacos:"
  echo "  curl -fsSL https://nacos.io/nacos-installer.sh | bash"
  echo "  nacos-setup"
  echo "  bin/startup.sh -m standalone"
  exit 1
}

wait_nacos_ready() {
  for i in $(seq 1 30); do
    if curl -s -o /dev/null -w '' "http://$NACOS_HOST:$NACOS_PORT/nacos/actuator/health" 2>/dev/null; then
      echo " 就绪"
      return 0
    fi
    sleep 2
  done
  return 1
}

check_special_prerequisites() {
  echo ""
  echo "========== 检查特殊模块前置条件 =========="
  check_rocketmq
  check_seata
  check_ai
  check_rag
  check_kafka
  echo "=================================="
}

check_rocketmq() {
  # RocketMQ: 检查或自动启动
  if nc -z 127.0.0.1 9876 2>/dev/null; then
    echo "[Stream] ✓ RocketMQ 已运行"
    START_STREAM=true
  elif find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' 2>/dev/null | grep -q .; then
    echo "[Stream] RocketMQ 未运行，正在自动启动..."
    if start_rocketmq; then
      START_STREAM=true
    else
      echo "[Stream] ✗ RocketMQ 启动失败，跳过 Stream 模块"
    fi
  else
    echo "[Stream] ✗ RocketMQ 未运行且未安装，跳过 Stream 模块"
  fi
  if $START_STREAM; then
    create_rocketmq_topics
  fi
}

check_seata() {
  # Seata: MySQL + Seata Server
  local mysql_ok=false
  local seata_server_ok=false
  if mysql -u root -proot1234 -e "SELECT 1" &>/dev/null; then
    echo "[Seata] ✓ MySQL 已运行"
    mysql_ok=true
  else
    echo "[Seata] ✗ MySQL 未运行，跳过 Seata 模块"
  fi
  if nc -z 127.0.0.1 8091 2>/dev/null; then
    echo "[Seata] ✓ Seata Server 已运行"
    seata_server_ok=true
  elif $mysql_ok && [ -d "$HOME/github/seata" ]; then
    echo "[Seata] Seata Server 未运行，正在自动启动..."
    if start_seata_server; then
      seata_server_ok=true
    else
      echo "[Seata] ✗ Seata Server 启动失败"
    fi
  elif ! $mysql_ok; then
    :
  else
    echo "[Seata] ✗ Seata Server 未运行且源码不存在，跳过 Seata 模块"
  fi
  if $mysql_ok && $seata_server_ok; then
    START_SEATA=true
    echo "[Seata] 将启动 7 个微服务 (18081-18084 + 3 Dubbo)"
  fi
}

check_ai() {
  # AI: OPENAI_API_KEY / DEEPSEEK_API_KEY + PostgreSQL (ChatMemory JDBC)
  if [ -n "$OPENAI_API_KEY" ] || [ -n "$DEEPSEEK_API_KEY" ]; then
    local keys=""
    [ -n "$OPENAI_API_KEY" ] && keys="OPENAI_API_KEY"
    [ -n "$DEEPSEEK_API_KEY" ] && keys="${keys:+$keys, }DEEPSEEK_API_KEY"
    if pg_isready -h localhost -p 5432 &>/dev/null; then
      echo "[AI] ✓ 已配置: $keys，PostgreSQL 已运行"
      START_AI=true
    else
      echo "[AI] ✗ 已配置: $keys，但 PostgreSQL 未运行，跳过 AI 模块"
      echo "[AI]   ChatMemory JDBC 需要 PostgreSQL，请启动: brew services start postgresql"
    fi
  else
    echo "[AI] ✗ OPENAI_API_KEY 和 DEEPSEEK_API_KEY 均未设置，跳过 AI 模块"
  fi
}

check_rag() {
  # RAG: PostgreSQL + OPENAI_API_KEY
  if [ -n "$OPENAI_API_KEY" ] && pg_isready -h localhost -p 5432 &>/dev/null; then
    echo "[RAG] ✓ PostgreSQL 已运行，将启动 AI RAG 模块 (端口 8889)"
    START_RAG=true
  elif [ -n "$OPENAI_API_KEY" ]; then
    echo "[RAG] ✗ PostgreSQL 未运行，跳过 AI RAG 模块"
    echo "[RAG]   如需启用，请安装: brew install postgresql pgvector && brew services start postgresql"
    echo "[RAG]   然后初始化: psql -U postgres -f cloud-ai-rag-sample/init_ai_demo.sql"
  else
    echo "[RAG] ✗ 未配置 OPENAI_API_KEY，跳过 AI RAG 模块"
  fi
}

check_kafka() {
  # Kafka: 检查集群是否运行
  if nc -z 127.0.0.1 9092 2>/dev/null; then
    echo "[Kafka] ✓ Kafka 集群已运行 (端口 9092)"
    START_KAFKA=true
    create_kafka_topics
  else
    echo "[Kafka] ✗ Kafka 集群未运行，跳过 Kafka 模块"
    echo "[Kafka]   如需启用，请参考 references/kafka.md 部署 Kafka 4.x 集群"
  fi
}

start_rocketmq() {
  local rocketmq_home
  rocketmq_home=$(find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' | sort -V | tail -1)
  if [ -z "$rocketmq_home" ] || [ ! -d "$rocketmq_home" ]; then
    echo "[RocketMQ] ✗ 未在 $HOME 下找到 rocketmq-* 目录，请先下载安装"
    return 1
  fi
  printf '[RocketMQ] 启动 NameServer ...'
  cd "$rocketmq_home"
  nohup bin/mqnamesrv > "$LOG_DIR/rocketmq-namesrv.log" 2>&1 &
  sleep 5
  if nc -z 127.0.0.1 9876 2>/dev/null; then
    echo " ✓"
  else
    echo " ✗ 请查看日志: $LOG_DIR/rocketmq-namesrv.log"
    return 1
  fi
  printf '[RocketMQ] 启动 Broker ...'
  nohup bin/mqbroker -n localhost:9876 > "$LOG_DIR/rocketmq-broker.log" 2>&1 &
  sleep 10
  if nc -z 127.0.0.1 10911 2>/dev/null; then
    echo " ✓"
  else
    echo " ✗ 请查看日志: $LOG_DIR/rocketmq-broker.log"
    return 1
  fi
  cd "$BASE_DIR"
}

create_rocketmq_topics() {
  local rocketmq_home
  rocketmq_home=$(find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' | sort -V | tail -1)
  if [ -z "$rocketmq_home" ] || [ ! -x "$rocketmq_home/bin/mqadmin" ]; then
    echo "[Stream] ✗ 未找到 mqadmin，跳过 Topic 创建"
    return
  fi
  echo "[Stream] 检查并创建 RocketMQ Topic 和消费组 ..."
  local mqadmin="$rocketmq_home/bin/mqadmin"
  # NORMAL Topics
  for pair in \
    "stream-demo-topic|stream-demo-consumer-group" \
    "stream-demo-topic2|stream-demo-consumer-group2" \
    "stream-transform-topic|stream-transform-group"; do
    IFS='|' read -r topic group <<< "$pair"
    $mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t "$topic" -a +message.type=NORMAL &>/dev/null \
      && echo "  ✓ Topic [$topic]" || true
    $mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g "$group" &>/dev/null \
      && echo "  ✓ Group [$group]" || true
  done
  # DELAY Topic
  $mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-delay-topic -a +message.type=DELAY &>/dev/null \
    && echo "  ✓ Topic [stream-delay-topic] (DELAY)" || true
  $mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-delay-group &>/dev/null \
    && echo "  ✓ Group [stream-delay-group]" || true
  # FIFO Topic
  $mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-fifo-topic -a +message.type=FIFO &>/dev/null \
    && echo "  ✓ Topic [stream-fifo-topic] (FIFO)" || true
  $mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-fifo-group &>/dev/null \
    && echo "  ✓ Group [stream-fifo-group]" || true
  # TRANSACTION Topic
  $mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t stream-tx-topic -a +message.type=TRANSACTION &>/dev/null \
    && echo "  ✓ Topic [stream-tx-topic] (TRANSACTION)" || true
  $mqadmin updateSubGroup -n localhost:9876 -c DefaultCluster -g stream-tx-group &>/dev/null \
    && echo "  ✓ Group [stream-tx-group]" || true
}

start_seata_server() {
  local seata_src="$HOME/github/seata"
  if [ ! -d "$seata_src" ]; then
    echo "[Seata Server] ✗ 未找到 $seata_src，请先克隆源码"
    return 1
  fi
  printf '[Seata Server] 启动中 ...'
  cd "$seata_src"
  nohup ./mvnw -pl server spring-boot:run > "$LOG_DIR/seata-server.log" 2>&1 &
  cd "$BASE_DIR"
  local ready=false
  for i in $(seq 1 30); do
    if nc -z 127.0.0.1 8091 2>/dev/null; then
      echo " ✓ (端口 8091)"
      ready=true
      break
    fi
    sleep 1
  done
  if ! $ready; then
    echo " ✗ 请查看日志: $LOG_DIR/seata-server.log"
    return 1
  fi
}

create_kafka_topics() {
  local kafka_home
  kafka_home=$(find "$HOME" -maxdepth 1 -type d -name 'kafka_*' | sort -V | tail -1)
  if [ -n "$kafka_home" ] && [ -x "$kafka_home/bin/kafka-topics.sh" ]; then
    echo "[Kafka] 检查并创建 Topic (3分区 3副本) ..."
    for topic in share-demo-topic share-demo-topic-explicit tx-demo-topic; do
      "$kafka_home/bin/kafka-topics.sh" --bootstrap-server localhost:9092 \
        --create --topic "$topic" --partitions 3 --replication-factor 3 --if-not-exists 2>/dev/null \
        && echo "  ✓ Topic [$topic] 已就绪" || true
    done
  fi
}

start_single() {
  local arr_name="$1"
  eval "local first_elem=\"\${${arr_name}[0]}\""
  IFS='|' read -r module_dir display_name port <<< "$first_elem"
  start_module "$module_dir" "$display_name" "$port"
}

start_seata_services() {
  # 按依赖顺序启动：account/storage（基础层）→ order（依赖 account）→ business（依赖 storage + order）
  echo "[Seata] 按依赖顺序启动 7 个微服务..."

  # 第一层：account-dubbo/account + storage-dubbo/storage（无下游依赖，基础层）
  for entry in \
    "cloud-seata-sample/account-dubbo-service|seata-account-dubbo|50071" \
    "cloud-seata-sample/account-service|seata-account|18084" \
    "cloud-seata-sample/storage-dubbo-service|seata-storage-dubbo|50072" \
    "cloud-seata-sample/storage-service|seata-storage|18082"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    start_module "$module_dir" "$display_name" "$port"
  done

  # 第二层：order-dubbo（依赖 account-dubbo）+ order-service（依赖 account-service）
  for entry in \
    "cloud-seata-sample/order-dubbo-service|seata-order-dubbo|50073" \
    "cloud-seata-sample/order-service|seata-order|18083"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    start_module "$module_dir" "$display_name" "$port"
  done

  # 第三层：business-service（依赖 storage + order）
  IFS='|' read -r module_dir display_name port <<< "cloud-seata-sample/business-service|seata-business|18081"
  start_module "$module_dir" "$display_name" "$port"
}

start_module() {
  local module_dir="$1"
  local display_name="$2"
  local port="$3"
  local pid_file="$PID_DIR/$display_name.pid"
  local log_file="$LOG_DIR/$display_name.log"

  # 检查是否已在运行（通过 PID 文件）
  if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "[$display_name] 已在运行 (PID: $(cat "$pid_file"))"
    return
  fi

  # 检查端口是否已被占用（服务可能通过其他方式启动）
  if [ "$port" != "-" ]; then
    if curl -s -o /dev/null --connect-timeout 2 "http://localhost:$port" 2>/dev/null; then
      echo "[$display_name] 端口 $port 已被占用，跳过启动"
      return
    fi
  else
    # 无 HTTP 端口的模块（短生命周期，执行一次即退出）
    # 如果日志已有内容说明之前已成功运行，跳过重启避免日志被清空
    if [ -f "$log_file" ] && [ -s "$log_file" ]; then
      echo "[$display_name] 已完成运行（日志保留）"
      return
    fi
  fi

  printf '[%s] 启动中 (port: %s) ...' "$display_name" "$port"
  cd "$BASE_DIR"
  nohup ./mvnw -pl "$module_dir" spring-boot:run > "$log_file" 2>&1 &
  local pid=$!
  echo "$pid" > "$pid_file"

  # 等待启动（最多 60 秒，mvn spring-boot:run 比 java -jar 慢一些）
  local ready=false
  for i in $(seq 1 60); do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo " 失败! 请查看日志: $log_file"
      rm -f "$pid_file"
      return 1
    fi
    if [ "$port" = "-" ]; then
      # 无 HTTP 端口的模块，等待日志中出现关键输出确认 CommandLineRunner 已执行
      if [ -f "$log_file" ] && grep -qE "Started|started|result:" "$log_file" 2>/dev/null; then
        echo " 已启动 (PID: $pid)"
        ready=true
        break
      fi
    elif curl -s -o /dev/null -w '' "http://localhost:$port" 2>/dev/null; then
      echo " 成功 (PID: $pid, port: $port)"
      ready=true
      break
    fi
    sleep 1
  done

  if [ "$ready" = false ]; then
    echo " 超时! 请查看日志: $log_file"
    rm -f "$pid_file"
    return 1
  fi
}

cmd_start() {
  echo "========== 启动所有服务 =========="
  check_java
  echo ""
  check_nacos
  echo ""
  check_special_prerequisites
  echo ""
  build_all
  echo ""
  for entry in "${MODULES[@]}"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    start_module "$module_dir" "$display_name" "$port"
  done
  $START_STREAM && start_single STREAM_MODULE
  $START_SEATA && start_seata_services
  $START_AI && start_single AI_MODULE
  $START_RAG && start_single RAG_MODULE
  $START_KAFKA && start_single KAFKA_MODULE
  echo ""
  status_all
  demo_urls
}

stop_all() {
  echo "正在停止所有服务..."

  # 收集所有模块名（含特殊模块）
  local all_modules=("${MODULES[@]}" "${STREAM_MODULE[@]}" "${SEATA_MODULES[@]}" "${AI_MODULE[@]}" "${RAG_MODULE[@]}" "${KAFKA_MODULE[@]}")

  # 第一阶段：并发停止通过 PID 文件启动的进程
  # 1a. 先对所有进程发送 SIGTERM
  local -a pending_pids=()
  local -a pending_names=()
  for pid_file in "$PID_DIR"/*.pid; do
    [ -f "$pid_file" ] || continue
    local name=$(basename "$pid_file" .pid)
    local pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      pending_pids+=("$pid")
      pending_names+=("$name")
      printf '[%s] 发送停止信号 (PID: %s)\n' "$name" "$pid"
    else
      echo "[$name] 未在运行"
    fi
    rm -f "$pid_file"
  done

  # 1b. 统一等待所有进程退出（最多 15 秒）
  if [ ${#pending_pids[@]} -gt 0 ]; then
    echo "等待 ${#pending_pids[@]} 个进程退出..."
    for i in $(seq 1 15); do
      local all_gone=true
      for pid in "${pending_pids[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
          all_gone=false
          break
        fi
      done
      if $all_gone; then break; fi
      sleep 1
    done

    # 1c. 强杀仍未退出的进程
    for idx in "${!pending_pids[@]}"; do
      local pid="${pending_pids[$idx]}"
      local name="${pending_names[$idx]}"
      if kill -0 "$pid" 2>/dev/null; then
        kill -9 "$pid" 2>/dev/null || true
        echo "[$name] 强制终止 (PID: $pid)"
      fi
    done
    echo "所有 PID 文件记录的进程已停止"
  fi

  # 第二阶段：并发扫描并停止外部启动的项目模块进程（如 IDE、手动 java -jar 等）
  echo ""
  echo "扫描外部启动的模块进程..."
  local found_external=false
  local -a external_pids=()
  local -a external_names=()
  for entry in "${all_modules[@]}"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    local killed
    killed=$(pgrep -f "${module_dir}.*(spring-boot:run|\.jar)" 2>/dev/null || true)
    if [ -n "$killed" ]; then
      found_external=true
      for pid in $killed; do
        external_pids+=("$pid")
        external_names+=("$display_name")
      done
      echo "$killed" | xargs kill 2>/dev/null || true
    fi
  done

  # 统一等待外部进程退出（最多 5 秒）
  if [ ${#external_pids[@]} -gt 0 ]; then
    for i in $(seq 1 5); do
      local all_gone=true
      for pid in "${external_pids[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then all_gone=false; break; fi
      done
      if $all_gone; then break; fi
      sleep 1
    done
    # 强杀剩余
    for pid in "${external_pids[@]}"; do
      if kill -0 "$pid" 2>/dev/null; then
        kill -9 "$pid" 2>/dev/null || true
      fi
    done
    echo "已停止外部进程 (PID: ${external_pids[*]})"
  fi
  if ! $found_external; then
    echo "未发现外部启动的模块进程"
  fi

  # 停止 RocketMQ 和 Seata Server
  pkill -f "rocketmq" 2>/dev/null || true
  pkill -f "seata.*spring-boot:run" 2>/dev/null || true
  sleep 1
  pgrep -f "rocketmq" 2>/dev/null | xargs kill -9 2>/dev/null || true
  rm -rf "$LOG_DIR" "$PID_DIR"
  echo "所有服务已停止，logs 和 .pids 目录已清理"
}

install_all() {
  echo "========== 检查并安装中间件 =========="

  # Nacos (复用 check_nacos)
  echo ""
  check_nacos

  # RocketMQ
  echo ""
  echo "--- RocketMQ ---"
  if nc -z 127.0.0.1 9876 2>/dev/null; then
    echo "✓ RocketMQ 已运行"
  elif find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' 2>/dev/null | grep -q .; then
    echo "✓ RocketMQ 已安装（未运行）"
  else
    echo "正在下载 RocketMQ 5.5.0..."
    cd "$HOME"
    curl -O https://dist.apache.org/repos/dist/release/rocketmq/5.5.0/rocketmq-all-5.5.0-bin-release.zip
    unzip -o rocketmq-all-5.5.0-bin-release.zip -d "$HOME"
    echo "✓ RocketMQ 已安装到 $(find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' | sort -V | tail -1)"
    cd "$BASE_DIR"
  fi

  # MySQL
  echo ""
  echo "--- MySQL ---"
  if mysql -u root -proot1234 -e "SELECT 1" &>/dev/null; then
    echo "✓ MySQL 已运行且密码正确"
  elif command -v mysql &>/dev/null; then
    echo "✗ MySQL 已安装但密码不是 root1234，请手动执行: mysqladmin -u root password 'root1234'"
  else
    echo "正在安装 MySQL..."
    brew install mysql
    mysql.server start
    mysqladmin -u root password 'root1234'
    echo "✓ MySQL 已安装并设置密码"
  fi

  # Seata Server
  echo ""
  echo "--- Seata Server ---"
  if nc -z 127.0.0.1 8091 2>/dev/null; then
    echo "✓ Seata Server 已运行"
  elif [ -d "$HOME/github/seata" ]; then
    echo "✓ Seata Server 源码已存在（未运行）"
  else
    echo "正在下载 Seata 源码..."
    mkdir -p "$HOME/github"
    curl -L -o /tmp/seata-2.x.zip https://github.com/javahongxi/seata/archive/refs/heads/2.x.zip
    unzip -o /tmp/seata-2.x.zip -d "$HOME/github"
    mv "$HOME/github/seata-2.x" "$HOME/github/seata"
    rm -f /tmp/seata-2.x.zip
    echo "正在构建 Seata Server（首次构建耗时较长）..."
    cd "$HOME/github/seata"
    ./mvnw clean install -DskipTests -q
    echo "✓ Seata Server 已构建"
    cd "$BASE_DIR"
  fi

  # MySQL 数据库初始化
  echo ""
  echo "--- Seata 数据库初始化 ---"
  if mysql -u root -proot1234 -e "SELECT 1" &>/dev/null; then
    mysql -u root -proot1234 -e "CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    mysql -u root -proot1234 seata < "$BASE_DIR/cloud-seata-sample/all.sql"
    echo "✓ Seata 数据库已初始化"
  else
    echo "✗ MySQL 未就绪，跳过数据库初始化"
  fi

  echo ""
  echo "=========================================="
  echo "  中间件检查/安装完成"
  echo "=========================================="

  # 打包项目模块
  echo ""
  build_all
}

cmd_infra() {
  echo "========== 仅启动中间件（不启动微服务） =========="
  check_java
  echo ""
  check_nacos
  echo ""
  check_special_prerequisites
  echo ""
  echo "=========================================="
  echo "  中间件已就绪，微服务未启动"
  echo "  可使用 docker compose up -d 在 Docker 中启动微服务"
  echo "  或使用 $0 start 在本地启动所有微服务"
  echo "=========================================="
}

build_all() {
  echo "========== 打包所有模块 =========="
  cd "$BASE_DIR"
  ./mvnw clean package -DskipTests -q
  if [ $? -eq 0 ]; then
    echo "✓ 所有模块打包成功"
  else
    echo "✗ 打包失败"
    exit 1
  fi
}

# 验证计数器
VERIFY_PASS=0
VERIFY_FAIL=0

# 执行 curl 请求并记录结果: verify_url url desc
verify_url() {
  local url="$1"
  local desc="$2"
  echo ""
  echo "[$desc]"
  echo "  URL: $url"
  local response
  response=$(curl -s -w '\n%{http_code}' --max-time 10 "$url" 2>/dev/null)
  local http_code
  http_code=$(echo "$response" | tail -n1)
  local body
  body=$(echo "$response" | sed '$d')
  echo "  响应: $body"
  echo "  HTTP Status: $http_code"
  if [ "$http_code" = "200" ]; then
    VERIFY_PASS=$((VERIFY_PASS + 1))
  else
    VERIFY_FAIL=$((VERIFY_FAIL + 1))
    VERIFY_FAILED_LIST+=("[$desc] $url (HTTP $http_code)")
  fi
}

# 验证日志中包含指定内容: verify_log log_file keyword desc
verify_log() {
  local log_file="$1"
  local keyword="$2"
  local desc="$3"
  echo ""
  echo "[$desc]"
  echo "  日志文件: $log_file"
  if [ ! -f "$log_file" ]; then
    echo "  日志文件不存在!"
    VERIFY_FAIL=$((VERIFY_FAIL + 1))
    VERIFY_FAILED_LIST+=("[$desc] 日志文件不存在")
    return
  fi
  local match
  match=$(grep "$keyword" "$log_file" 2>/dev/null | tail -1)
  if [ -n "$match" ]; then
    echo "  匹配: $match"
    VERIFY_PASS=$((VERIFY_PASS + 1))
  else
    echo "  未找到关键字: $keyword"
    VERIFY_FAIL=$((VERIFY_FAIL + 1))
    VERIFY_FAILED_LIST+=("[$desc] 日志中未找到 $keyword")
  fi
}

# README 中的演示 URL 列表
demo_urls() {
  VERIFY_PASS=0
  VERIFY_FAIL=0
  VERIFY_FAILED_LIST=()

  # Nacos Discovery 服务发现验证
  echo ""
  echo "========== Nacos Discovery 验证 =========="
  verify_url "http://localhost:8760/discovery/services" "Nacos Discovery 获取服务列表"
  echo "=================================="

  # 普通 Web 服务注册与发现
  echo ""
  echo "========== 普通 Web 服务注册与发现 =========="
  local web_urls=(
    "http://localhost:8766/hi?name=hongxi|直接访问 consumer (consumer → provider)"
    "http://localhost:8764/consumer-sample/hi?name=hongxi|通过网关访问 consumer (gateway → consumer → provider)"
  )
  for entry in "${web_urls[@]}"; do
    IFS='|' read -r url desc <<< "$entry"
    verify_url "$url" "$desc"
  done
  echo "=================================="

  # Reactive Web 服务注册与发现
  echo ""
  echo "========== Reactive Web 服务注册与发现 =========="
  local reactive_urls=(
    "http://localhost:8763/hi?name=hongxi|直接访问 consumer-reactive (consumer-reactive → provider-reactive)"
    "http://localhost:8764/consumer-reactive-sample/hi?name=hongxi|通过网关访问 consumer-reactive (gateway → consumer-reactive → provider-reactive)"
  )
  for entry in "${reactive_urls[@]}"; do
    IFS='|' read -r url desc <<< "$entry"
    verify_url "$url" "$desc"
  done
  echo "=================================="

  # Dubbo 服务注册与发现
  echo ""
  echo "========== Dubbo 服务注册与发现 =========="
  local dubbo_urls=(
    "http://localhost:8766/dubbo?name=hongxi|直接访问 consumer (consumer → provider-dubbo)"
    "http://localhost:8764/consumer-sample/dubbo?name=hongxi|通过网关访问 consumer (gateway → consumer → provider-dubbo)"
    "http://localhost:8763/dubbo?name=hongxi|直接访问 consumer-reactive (consumer-reactive → provider-dubbo)"
    "http://localhost:8764/consumer-reactive-sample/dubbo?name=hongxi|通过网关访问 consumer-reactive (gateway → consumer-reactive → provider-dubbo)"
  )
  for entry in "${dubbo_urls[@]}"; do
    IFS='|' read -r url desc <<< "$entry"
    verify_url "$url" "$desc"
  done
  echo "=================================="

  # gRPC 服务注册与发现
  echo ""
  echo "========== gRPC 服务注册与发现 =========="
  local grpc_urls=(
    "http://localhost:8766/grpc?name=hongxi|直接访问 consumer (consumer → grpc-server)"
    "http://localhost:8764/consumer-sample/grpc?name=hongxi|通过网关访问 consumer (gateway → consumer → grpc-server)"
  )
  for entry in "${grpc_urls[@]}"; do
    IFS='|' read -r url desc <<< "$entry"
    verify_url "$url" "$desc"
  done
  echo "=================================="

  # Dubbo REST 接口验证
  echo ""
  echo "========== Dubbo REST 接口验证 =========="
  local dubbo_rest_urls=(
    "http://localhost:50051/api/hello/lily|直接访问 provider-dubbo (dubbo rest)"
    "http://localhost:8764/provider-dubbo-sample/api/hello/lily|通过网关访问 provider-dubbo (dubbo rest)"
  )
  for entry in "${dubbo_rest_urls[@]}"; do
    IFS='|' read -r url desc <<< "$entry"
    verify_url "$url" "$desc"
  done
  echo "=================================="

  # Trace 链路追踪验证
  echo ""
  echo "========== Trace 链路追踪验证 =========="
  echo "  提示: 执行 bash .qoder/skills/demo-spring-cloud/scripts/verify-trace.sh 验证五条链路 trace 传播 (Web→Web / Web→gRPC / Web→Dubbo / Reactive→Reactive / Reactive→Dubbo)"
  echo "=================================="

  # Nacos Config 验证
  echo ""
  echo "========== Nacos Config 验证 =========="
  verify_url "http://localhost:8761/actuator/health" "Nacos Config 模块健康检查"
  echo "=================================="

  # Stream 模块验证
  if [ -f "$PID_DIR/stream.pid" ] && kill -0 "$(cat "$PID_DIR/stream.pid")" 2>/dev/null; then
    echo ""
    echo "========== Stream 模块验证 =========="
    verify_url "http://localhost:8767/actuator/health" "Stream 模块健康检查"
    verify_log "$LOG_DIR/stream.log" "result: true" "Stream 模块消息收发"
    echo "=================================="
  fi

  # Seata 服务验证
  if [ -f "$PID_DIR/seata-business.pid" ] && kill -0 "$(cat "$PID_DIR/seata-business.pid")" 2>/dev/null; then
    echo ""
    echo "========== Seata 分布式事务验证 =========="
    local seata_urls=(
      "http://localhost:18081/actuator/health|business-service 健康检查"
      "http://localhost:18082/actuator/health|storage-service 健康检查"
      "http://localhost:18083/actuator/health|order-service 健康检查"
      "http://localhost:18084/actuator/health|account-service 健康检查"
    )
    for entry in "${seata_urls[@]}"; do
      IFS='|' read -r url desc <<< "$entry"
      verify_url "$url" "$desc"
    done
    # Dubbo 服务通过日志验证
    verify_log "$LOG_DIR/seata-storage-dubbo.log" "Started" "storage-dubbo-service 启动验证"
    verify_log "$LOG_DIR/seata-order-dubbo.log" "Started" "order-dubbo-service 启动验证"
    verify_log "$LOG_DIR/seata-account-dubbo.log" "Started" "account-dubbo-service 启动验证"
    echo "=================================="
  fi

  # AI 模块验证
    if [ -f "$PID_DIR/ai.pid" ] && kill -0 "$(cat "$PID_DIR/ai.pid")" 2>/dev/null || curl -s -o /dev/null --connect-timeout 2 "http://localhost:8888/actuator/health" 2>/dev/null; then
      echo ""
      echo "========== Spring AI 模块验证 =========="
      verify_url "http://localhost:8888/actuator/health" "AI 模块健康检查"
      echo "=================================="
    fi

    # AI RAG 模块验证
    if [ -f "$PID_DIR/ai-rag.pid" ] && kill -0 "$(cat "$PID_DIR/ai-rag.pid")" 2>/dev/null || curl -s -o /dev/null --connect-timeout 2 "http://localhost:8889/actuator/health" 2>/dev/null; then
      echo ""
      echo "========== Spring AI RAG 模块验证 =========="
      verify_url "http://localhost:8889/actuator/health" "AI RAG 模块健康检查"
      echo "=================================="
    fi

    # Kafka 模块验证
    if [ -f "$PID_DIR/kafka-sample.pid" ] && kill -0 "$(cat "$PID_DIR/kafka-sample.pid")" 2>/dev/null; then
      echo ""
      echo "========== Kafka 4.x 模块验证 =========="
      verify_url "http://localhost:8768/actuator/health" "Kafka 模块健康检查"
      echo "=================================="
    fi

  # 汇总验证结果
  echo ""
  echo "=========================================="
  echo "  验证结果汇总: 通过 $VERIFY_PASS 项, 失败 $VERIFY_FAIL 项"
  echo "=========================================="
  if [ "$VERIFY_FAIL" -eq 0 ]; then
    echo ""
    echo "  ★ 全部验证通过! 所有服务运行正常 ★"
    echo ""
    
    # 显示后续可深入验证的功能提示
    echo "💡 当前已验证: 服务注册发现、健康检查、基础调用链路"
    echo ""
    echo "📌 还可深入验证以下高级功能:"
    echo ""
    echo "  1️⃣  Trace 链路追踪:"
    echo "     • Web→Web / Web→gRPC / Web→Dubbo trace ID 自动传播"
    echo "     • Reactive→Reactive (WebClient 手动传递) / Reactive→Dubbo trace 传播"
    echo "     → 使用 demo-spring-cloud skill 执行 scripts/verify-trace.sh"
    echo ""
    echo "  2️⃣  Nacos Config 动态配置 (端口 8761):"
    echo "     • 配置发布/读取/删除"
    echo "     • @NacosConfig 注解注入与动态刷新"
    echo "     • @ConfigurationProperties + @Value + @RefreshScope"
    echo "     → 使用 demo-spring-cloud skill 验证 Nacos Config 动态配置"
    echo ""
    echo "  3️⃣  Sentinel 限流与熔断降级:"
    echo "     • 网关限流: Nacos 配置限流规则，验证限流效果"
    echo "     • 应用级限流: consumer 接口 QPS 限流 (资源名 /hi)"
    echo "     • 应用级熔断: Feign/RestTemplate 出站调用 fallback"
    echo "     → 使用 demo-spring-cloud skill 验证 Sentinel 限流与熔断"
    echo ""
    echo "  4️⃣  Stream 消息收发 (RocketMQ):"
    echo "     • 基础消费: StreamBridge → topic → Consumer"
    echo "     • 定时消息源: Supplier 每秒自动发送"
    echo "     • 消息处理管道: Function 转换管道 (REST → toUpperCase → output)"
    echo "     • 延迟消息: StreamBridge + DELAY header 延迟投递"
    echo "     • 顺序消息: StreamBridge + ORDER_KEY 顺序消费"
    echo "     • 事务消息: StreamBridge + TransactionListener 两阶段提交"
    echo "     → 使用 demo-spring-cloud skill 执行 scripts/verify-stream.sh"
    echo ""
    echo "  5️⃣  Seata 分布式事务 (7 个子模块, 端口 18081-18084 + 3 Dubbo):"
    echo "     • 全局事务回滚/提交场景"
    echo "     • Feign / RestTemplate / Dubbo 三种调用链路"
    echo "     • Xid 传递与数据一致性验证"
    echo "     → 使用 demo-spring-cloud skill 执行 scripts/verify-seata.sh"
    echo ""
    echo "  6️⃣  Spring AI 深度功能 (端口 8888):"
    echo "     • 聊天对话、流式输出、结构化提取"
    echo "     • Tool Calling、ReAct Agent"
    echo "     • 多模态视觉识别 (6种场景)"
    echo "     • DeepSeek 多提供商集成 (需配置 DEEPSEEK_API_KEY)"
    echo "     • ChatMemory 多轮对话记忆 (JDBC 持久化到 PostgreSQL)"
    echo "     • PromptTemplate 提示词模板 (产品描述/代码解释/自定义模板)"
    echo "     → 使用 demo-spring-cloud skill 进行验证"
    echo ""
    echo "  7️⃣  Spring AI RAG 模块 (端口 8889):"
    echo "     • RAG 检索增强生成: 文档摄入 → 向量化存储 → 相似性检索 → 增强回答"
    echo "     → 使用 demo-spring-cloud skill 进行验证"
    echo ""
    echo "  8️⃣  Kafka 4.x 集群消息收发 (端口 8768):"
    echo "     • 启动后 ApplicationRunner 自动发送传统 Consumer Group 消息"
    echo "     • Share Group 隐式/显式确认消息 (REST 接口触发)"
    echo "     • 事务消息: 原子发送 + 提交/回滚 (read_committed 隔离)"
    echo "     → 使用 demo-spring-cloud skill 进行验证"
    echo ""
  else
    echo ""
    echo "  以下验证项失败:"
    for failed in "${VERIFY_FAILED_LIST[@]}"; do
      echo "    - $failed"
    done
    echo ""
  fi
}

logs_all() {
  local module_name="$1"
  if [ -z "$module_name" ]; then
    echo "用法: $0 logs <模块名>"
    echo ""
    echo "可用模块:"
    echo "  核心模块: nacos-discovery, gateway, provider, provider-reactive, provider-dubbo,"
    echo "            grpc-server, consumer, consumer-reactive, nacos-config"
    echo "  特殊模块: ai, ai-rag, stream, kafka-sample, seata-business, seata-storage, seata-order, seata-account,"
    echo "            seata-storage-dubbo, seata-order-dubbo, seata-account-dubbo"
    echo "  基础设施: rocketmq-namesrv, rocketmq-broker, seata-server"
    return
  fi
  local log_file="$LOG_DIR/$module_name.log"
  if [ -f "$log_file" ]; then
    tail -f "$log_file"
  else
    echo "日志文件不存在: $log_file"
    echo "可用模块的日志:"
    ls -1 "$LOG_DIR"/*.log 2>/dev/null | sed "s|$LOG_DIR/||;s|\.log||" | sed 's/^/  /'
  fi
}

status_all() {
  echo "========== 服务状态 =========="
  printf "%-22s %-12s %s\n" "模块" "状态" "PID"
  printf "%-22s %-12s %s\n" "----" "----" "---"
  # 辅助函数: 检查单个模块状态
  _check_status() {
    local display_name="$1"
    local port="$2"
    local pid_file="$PID_DIR/$display_name.pid"
    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
      printf "%-22s %-12s %s\n" "$display_name" "运行中" "$(cat "$pid_file")"
    elif [ "$port" != "-" ] && curl -s -o /dev/null --connect-timeout 2 "http://localhost:$port" 2>/dev/null; then
      printf "%-22s %-12s %s\n" "$display_name" "运行中(外部)" "-"
    else
      printf "%-22s %-12s %s\n" "$display_name" "已停止" "-"
      rm -f "$pid_file"
    fi
  }
  for entry in "${MODULES[@]}"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    _check_status "$display_name" "$port"
  done
  # 特殊模块
  local all_special=("${STREAM_MODULE[0]}" "${SEATA_MODULES[@]}" "${AI_MODULE[0]}" "${RAG_MODULE[0]}" "${KAFKA_MODULE[0]}")
  for entry in "${all_special[@]}"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    _check_status "$display_name" "$port"
  done
  echo "=============================="
}

cmd_seata() {
  echo "========== 启动 Seata 分布式事务 (7个模块) =========="
  check_java
  echo ""
  check_nacos
  echo ""
  echo "========== 检查 Seata 前置条件 =========="
  if ! mysql -u root -proot1234 -e "SELECT 1" &>/dev/null; then
    echo "[Seata] ✗ MySQL 未运行，请先运行: $0 install"
    exit 1
  fi
  echo "[Seata] ✓ MySQL 已运行"
  if ! nc -z 127.0.0.1 8091 2>/dev/null; then
    if [ -d "$HOME/github/seata" ]; then
      echo "[Seata] Seata Server 未运行，正在自动启动..."
      if ! start_seata_server; then
        echo "[Seata] ✗ Seata Server 启动失败"
        exit 1
      fi
    else
      echo "[Seata] ✗ Seata Server 未运行，请先启动 Seata Server"
      exit 1
    fi
  fi
  echo "[Seata] ✓ Seata Server 已运行"
  echo "=================================="
  echo "✓ 前置条件就绪"
  echo ""
  build_all
  echo ""
  start_seata_services
  echo ""
  echo "========== Seata 服务已启动 =========="
  echo "  Business:   http://localhost:18081"
  echo "  Order:      http://localhost:18083"
  echo "  Storage:    http://localhost:18082"
  echo "  Account:    http://localhost:18084"
  echo ""
  echo "验证:"
  echo "  curl http://localhost:18081/seata/rest"
  echo "  curl http://localhost:18081/seata/feign"
  echo "  curl http://localhost:18081/seata/dubbo"
}

show_help() {
  echo "用法: $0 {start|stop|install|infra|seata|build|verify|logs|status|help}"
  echo ""
  echo "命令说明:"
  echo "  start    启动所有服务（默认）"
  echo "  stop     停止所有服务（含 RocketMQ、Seata Server）"
  echo "  install  检查并安装中间件 + 打包模块"
  echo "  infra    仅启动中间件（配合 Docker 部署微服务时使用）"
  echo "  seata    仅启动 Seata 分布式事务 (7个模块)"
  echo "  build    打包所有模块"
  echo "  verify   执行验证（不启动，仅验证已运行的服务）"
  echo "  status   查看服务状态"
  echo "  logs     查看模块日志 (用法: $0 logs <模块名>)"
  echo "  help     显示此帮助信息"
}

# ===== 命令分发 =====
case "${1:-start}" in
  start)         cmd_start ;;
  stop)          stop_all ;;
  install)       install_all ;;
  infra)         cmd_infra ;;
  build)         build_all ;;
  verify)        demo_urls ;;
  logs)          logs_all "$2" ;;
  status)        status_all ;;
  seata)         cmd_seata ;;
  help|--help|-h) show_help ;;
  *)             show_help; exit 1 ;;
esac
