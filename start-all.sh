#!/bin/bash
#
# 启动所有服务模块（除 cloud-sample-api 外）
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
  # 1. Nacos Discovery
  "cloud-nacos-discovery-sample|nacos-discovery|8760"
  # 2. Gateway（验证 Web/Reactive/Dubbo/gRPC/REST 都需要）
  "cloud-gateway-sample|gateway|8764"
  # 3. Providers
  "cloud-provider-sample|provider|8765"
  "cloud-provider-reactive-sample|provider-reactive|8762"
  "cloud-provider-dubbo-sample|provider-dubbo|-"
  # 4. gRPC Server
  "cloud-grpc-server-sample|grpc-server|8090"
  # 5. Consumers
  "cloud-consumer-sample|consumer|8766"
  "cloud-consumer-reactive-sample|consumer-reactive|8763"
  # 6. 纯 Dubbo/gRPC Client
  "cloud-consumer-dubbo-sample|consumer-dubbo|-"
  "cloud-grpc-client-sample|grpc-client|-"
  # 7. Nacos Config
  "cloud-nacos-config-sample|nacos-config|8761"
)

# 特殊模块（需额外条件）
AI_MODULE=("cloud-ai-sample|ai|8080")
STREAM_MODULE=("cloud-stream-sample|stream|-")
SEATA_MODULES=(
  "cloud-seata-sample/business-service|seata-business|18081"
  "cloud-seata-sample/storage-service|seata-storage|18082"
  "cloud-seata-sample/order-service|seata-order|18083"
  "cloud-seata-sample/account-service|seata-account|18084"
)

# 特殊模块启动标记
START_SEATA=false
START_STREAM=false
START_AI=false

check_rocketmq() {
  nc -z 127.0.0.1 9876 2>/dev/null
}

check_mysql() {
  mysql -u root -proot1234 -e "SELECT 1" &>/dev/null
}

check_seata_server() {
  nc -z 127.0.0.1 8091 2>/dev/null
}

start_rocketmq() {
  local rocketmq_home
  rocketmq_home=$(find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' | sort -V | tail -1)
  if [ -z "$rocketmq_home" ] || [ ! -d "$rocketmq_home" ]; then
    echo "[RocketMQ] ✗ 未在 $HOME 下找到 rocketmq-* 目录，请先下载安装"
    return 1
  fi
  echo -n "[RocketMQ] 启动 NameServer ..."
  cd "$rocketmq_home"
  nohup bin/mqnamesrv > "$LOG_DIR/rocketmq-namesrv.log" 2>&1 &
  sleep 5
  if nc -z 127.0.0.1 9876 2>/dev/null; then
    echo " ✓"
  else
    echo " ✗ 请查看日志: $LOG_DIR/rocketmq-namesrv.log"
    return 1
  fi
  echo -n "[RocketMQ] 启动 Broker ..."
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

start_seata_server() {
  local seata_src="$HOME/github/seata"
  if [ ! -d "$seata_src" ]; then
    echo "[Seata Server] ✗ 未找到 $seata_src，请先克隆源码"
    return 1
  fi
  echo -n "[Seata Server] 启动中 ..."
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

check_special_prerequisites() {
  echo ""
  echo "========== 检查特殊模块前置条件 =========="

  # RocketMQ: 检查或自动启动
  if check_rocketmq; then
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

  # Seata: MySQL + Seata Server
  local mysql_ok=false
  local seata_server_ok=false
  if check_mysql; then
    echo "[Seata] ✓ MySQL 已运行"
    mysql_ok=true
  else
    echo "[Seata] ✗ MySQL 未运行，跳过 Seata 模块"
  fi
  if check_seata_server; then
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
    echo "[Seata] 将启动 4 个微服务 (18081-18084)"
  fi

  # AI: OPENAI_API_KEY
  if [ -n "$OPENAI_API_KEY" ]; then
    echo "[AI] ✓ OPENAI_API_KEY 已配置"
    START_AI=true
  else
    echo "[AI] ✗ OPENAI_API_KEY 未设置，跳过 AI 模块"
  fi

  echo "=================================="
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
    # 无 HTTP 端口的模块，跳过端口检查
    :
  fi

  echo -n "[$display_name] 启动中 (port: $port) ..."
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

start_ai_module() {
  IFS='|' read -r module_dir display_name port <<< "${AI_MODULE[0]}"
  local pid_file="$PID_DIR/$display_name.pid"
  local log_file="$LOG_DIR/$display_name.log"
  local jar_file="$BASE_DIR/$module_dir/target/${module_dir}.jar"

  if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "[$display_name] 已在运行 (PID: $(cat "$pid_file"))"
    return
  fi

  # 打包（如果 jar 不存在）
  if [ ! -f "$jar_file" ]; then
    echo -n "[$display_name] 打包中 ..."
    cd "$BASE_DIR"
    if ./mvnw -pl "$module_dir" package -DskipTests -q 2>/dev/null; then
      echo " 完成"
    else
      echo " 失败!"
      return 1
    fi
  fi

  echo -n "[$display_name] 启动中 (port: $port) ..."
  cd "$BASE_DIR"
  nohup java -jar "$jar_file" --spring.ai.openai.chat.options.model=qwen3.7-plus > "$log_file" 2>&1 &
  local pid=$!
  echo "$pid" > "$pid_file"

  local ready=false
  for i in $(seq 1 60); do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo " 失败! 请查看日志: $log_file"
      rm -f "$pid_file"
      return 1
    fi
    if curl -s -o /dev/null "http://localhost:$port/actuator/health" 2>/dev/null; then
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

start_stream_module() {
  IFS='|' read -r module_dir display_name port <<< "${STREAM_MODULE[0]}"
  start_module "$module_dir" "$display_name" "$port"
}

start_seata_services() {
  for entry in "${SEATA_MODULES[@]}"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    start_module "$module_dir" "$display_name" "$port"
  done
}

stop_all() {
  echo "正在停止所有服务..."
  for pid_file in "$PID_DIR"/*.pid; do
    [ -f "$pid_file" ] || continue
    local name=$(basename "$pid_file" .pid)
    local pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo -n "[$name] 停止中 (PID: $pid) ..."
      kill "$pid"
      # 等待进程退出（最多 10 秒）
      for i in $(seq 1 10); do
        if ! kill -0 "$pid" 2>/dev/null; then
          break
        fi
        sleep 1
      done
      if kill -0 "$pid" 2>/dev/null; then
        kill -9 "$pid" 2>/dev/null
      fi
      echo " 已停止"
    else
      echo "[$name] 未在运行"
    fi
    rm -f "$pid_file"
  done
  # 停止特殊模块（通过 PID 文件 + pkill 双保险）
  for pid_file in "$PID_DIR"/ai.pid "$PID_DIR"/stream.pid; do
    [ -f "$pid_file" ] || continue
    local name=$(basename "$pid_file" .pid)
    local pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo -n "[$name] 停止中 (PID: $pid) ..."
      kill "$pid"
      sleep 2
      kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
      echo " 已停止"
    fi
    rm -f "$pid_file"
  done
  for entry in "${SEATA_MODULES[@]}"; do
    IFS='|' read -r _ display_name _ <<< "$entry"
    local pid_file="$PID_DIR/$display_name.pid"
    [ -f "$pid_file" ] || continue
    local pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo -n "[$display_name] 停止中 (PID: $pid) ..."
      kill "$pid"
      sleep 2
      kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
      echo " 已停止"
    fi
    rm -f "$pid_file"
  done
  # 兜底：确保残留进程也被清理
  pkill -f "cloud-ai-sample" 2>/dev/null
  pkill -f "cloud-stream-sample" 2>/dev/null
  pkill -f "cloud-seata-sample" 2>/dev/null
  # 停止 RocketMQ 和 Seata Server
  pkill -f "rocketmq" 2>/dev/null
  sleep 1
  pgrep -f "rocketmq" 2>/dev/null | xargs kill -9 2>/dev/null
  pkill -f "seata.*spring-boot:run" 2>/dev/null
  rm -rf "$LOG_DIR" "$PID_DIR"
  echo "所有服务已停止，logs 和 .pids 目录已清理"
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
  verify_url "http://localhost:8760/discovery/instances" "Nacos Discovery 获取服务实例列表"
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

  # 纯 dubbo provider/consumer 演示验证
  echo ""
  echo "========== 纯 Dubbo provider/consumer 验证 =========="
  verify_log "$LOG_DIR/consumer-dubbo.log" "Hello, lily" "consumer-dubbo 调用 provider-dubbo"
  echo "=================================="

  # 纯 grpc server/client 演示验证
  echo ""
  echo "========== 纯 gRPC server/client 验证 =========="
  verify_log "$LOG_DIR/grpc-client.log" "Hello, lily" "grpc-client 调用 grpc-server"
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

  # Nacos Config 验证
  echo ""
  echo "========== Nacos Config 验证 =========="
  local nacos_config_urls=(
    "http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan|Nacos Config 发布配置"
    "http://localhost:8761/nacos/getConfig?dataId=my.city|Nacos Config 获取配置"
  )
  for entry in "${nacos_config_urls[@]}"; do
    IFS='|' read -r url desc <<< "$entry"
    verify_url "$url" "$desc"
  done
  echo "=================================="

  # AI 模块验证
  if [ -f "$PID_DIR/ai.pid" ] && kill -0 "$(cat "$PID_DIR/ai.pid")" 2>/dev/null; then
    echo ""
    echo "========== Spring AI 模块验证 =========="
    verify_url "http://localhost:8080/actuator/health" "AI 模块健康检查"
    echo "=================================="
  fi

  # Stream 模块验证
  if [ -f "$PID_DIR/stream.pid" ] && kill -0 "$(cat "$PID_DIR/stream.pid")" 2>/dev/null; then
    echo ""
    echo "========== Stream 模块验证 =========="
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
  else
    echo ""
    echo "  以下验证项失败:"
    for failed in "${VERIFY_FAILED_LIST[@]}"; do
      echo "    - $failed"
    done
    echo ""
  fi
}

status_all() {
  echo "========== 服务状态 =========="
  printf "%-22s %-8s %s\n" "模块" "状态" "PID"
  printf "%-22s %-8s %s\n" "----" "----" "---"
  for entry in "${MODULES[@]}"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    local pid_file="$PID_DIR/$display_name.pid"
    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
      printf "%-22s %-8s %s\n" "$display_name" "运行中" "$(cat "$pid_file")"
    else
      printf "%-22s %-8s %s\n" "$display_name" "已停止" "-"
      rm -f "$pid_file"
    fi
  done
  # 特殊模块
  local all_special=("${AI_MODULE[0]}" "${STREAM_MODULE[0]}" "${SEATA_MODULES[@]}")
  for entry in "${all_special[@]}"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    local pid_file="$PID_DIR/$display_name.pid"
    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
      printf "%-22s %-8s %s\n" "$display_name" "运行中" "$(cat "$pid_file")"
    else
      printf "%-22s %-8s %s\n" "$display_name" "已停止" "-"
      rm -f "$pid_file"
    fi
  done
  echo "=============================="
}

# Nacos 注册中心地址
NACOS_HOST="127.0.0.1"
NACOS_PORT="8848"

check_nacos() {
  echo -n "[Nacos] 检查注册中心 ($NACOS_HOST:$NACOS_PORT) ..."
  # 已运行
  if curl -s -o /dev/null -w '' "http://$NACOS_HOST:$NACOS_PORT/nacos/actuator/health" 2>/dev/null; then
    echo " 就绪"
    return 0
  fi
  # 尝试自动启动
  echo " 未运行，正在尝试自动启动..."
  # 在用户目录下查找 Nacos
  local nacos_bin
  local nacos_dir
  nacos_dir=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
  if [ -n "$nacos_dir" ] && [ -f "$nacos_dir/bin/startup.sh" ]; then
    nacos_bin="$nacos_dir/bin"
    bash "$nacos_bin/startup.sh" -m standalone
    echo -n "[Nacos] 等待启动就绪..."
    for i in $(seq 1 30); do
      if curl -s -o /dev/null -w '' "http://$NACOS_HOST:$NACOS_PORT/nacos/actuator/health" 2>/dev/null; then
        echo " 就绪"
        return 0
      fi
      sleep 2
    done
  fi
  echo " 失败!"
  echo "请先安装并启动 Nacos:"
  echo "  curl -fsSL https://nacos.io/nacos-installer.sh | bash"
  echo "  nacos-setup"
  echo "  bin/startup.sh -m standalone"
  exit 1
}

install_deps() {
  echo "正在安装依赖模块到本地仓库 ..."
  cd "$BASE_DIR"
  # 先安装根 pom，再安装依赖模块（cloud-commons、cloud-sample-api 依赖父 pom）
  if ./mvnw -N install -q && ./mvnw -pl cloud-commons,cloud-sample-api install -DskipTests -q; then
    echo "cloud-commons、cloud-sample-api 安装成功"
  else
    echo "依赖模块安装失败!"
    exit 1
  fi
}

install_all() {
  echo "========== 检查并安装中间件 =========="

  # Nacos
  echo ""
  echo "--- Nacos ---"
  if curl -s -o /dev/null -w '' "http://127.0.0.1:8848/nacos/actuator/health" 2>/dev/null; then
    echo "✓ Nacos 已运行"
  else
    # 在用户目录下查找 Nacos
    local nacos_bin
    local nacos_dir
    nacos_dir=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
    if [ -n "$nacos_dir" ] && [ -f "$nacos_dir/bin/startup.sh" ]; then
      nacos_bin="$nacos_dir/bin"
      echo "Nacos 已安装但未运行，正在启动..."
      bash "$nacos_bin/startup.sh" -m standalone
      echo "等待 Nacos 启动..."
      for i in $(seq 1 30); do
        if curl -s -o /dev/null -w '' "http://127.0.0.1:8848/nacos/actuator/health" 2>/dev/null; then
          echo "✓ Nacos 已启动"
          break
        fi
        sleep 2
      done
    else
      echo "正在安装 Nacos..."
      curl -fsSL https://nacos.io/nacos-installer.sh | bash
      echo "正在部署 Nacos..."
      nacos-setup
      echo "等待 Nacos 启动..."
      for i in $(seq 1 30); do
        if curl -s -o /dev/null -w '' "http://127.0.0.1:8848/nacos/actuator/health" 2>/dev/null; then
          echo "✓ Nacos 已启动"
          break
        fi
        sleep 2
      done
    fi
  fi

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
    echo "正在克隆 Seata 源码..."
    mkdir -p "$HOME/github"
    git clone https://github.com/javahongxi/seata.git "$HOME/github/seata"
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

logs_all() {
  local module_name="$1"
  if [ -z "$module_name" ]; then
    echo "用法: $0 logs <模块名>"
    echo ""
    echo "可用模块:"
    echo "  核心模块: nacos-discovery, gateway, provider, provider-reactive, provider-dubbo,"
    echo "            grpc-server, consumer, consumer-reactive, consumer-dubbo, grpc-client, nacos-config"
    echo "  特殊模块: ai, stream, seata-business, seata-storage, seata-order, seata-account"
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

clean_all() {
  echo "========== 清理构建产物 =========="
  cd "$BASE_DIR"
  ./mvnw clean -q
  echo "✓ 已清理所有 target 目录"
}

# 主逻辑
case "${1:-start}" in
  start)
    echo "========== 启动所有服务 =========="
    check_nacos
    echo ""
    check_special_prerequisites
    echo ""
    install_deps
    echo ""
    build_all
    echo ""
    for entry in "${MODULES[@]}"; do
      IFS='|' read -r module_dir display_name port <<< "$entry"
      start_module "$module_dir" "$display_name" "$port"
    done
    $START_SEATA && start_seata_services
    $START_STREAM && start_stream_module
    $START_AI && start_ai_module
    echo ""
    status_all
    demo_urls
    ;;
  stop)
    stop_all
    ;;
  restart)
    stop_all
    sleep 2
    echo ""
    echo "========== 重新启动所有服务 =========="
    check_nacos
    echo ""
    check_special_prerequisites
    echo ""
    install_deps
    echo ""
    build_all
    echo ""
    for entry in "${MODULES[@]}"; do
      IFS='|' read -r module_dir display_name port <<< "$entry"
      start_module "$module_dir" "$display_name" "$port"
    done
    $START_SEATA && start_seata_services
    $START_STREAM && start_stream_module
    $START_AI && start_ai_module
    echo ""
    status_all
    demo_urls
    ;;
  install)
    install_all
    ;;
  build)
    build_all
    ;;
  clean)
    clean_all
    ;;
  verify)
    demo_urls
    ;;
  logs)
    logs_all "$2"
    ;;
  status)
    status_all
    ;;
  *)
    echo "用法: $0 {start|stop|restart|install|build|clean|verify|logs|status}"
    exit 1
    ;;
esac
