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
AI_MODULE=("cloud-ai-sample|ai|8888")
STREAM_MODULE=("cloud-stream-sample|stream|8767")
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

check_java() {
  local min_version=17
  if ! command -v java &>/dev/null; then
    echo "[Java] ✗ 未检测到 Java"
    echo "[Java] 正在通过 Homebrew 安装 JDK $min_version ..."
    if command -v brew &>/dev/null; then
      brew install openjdk@$min_version
      echo "[Java] ✓ JDK $min_version 安装完成"
      echo "[Java] 请执行以下命令配置 PATH（或添加到 ~/.zshrc）:"
      echo "  sudo ln -sfn $(brew --prefix)/opt/openjdk@$min_version/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-$min_version.jdk"
      echo "  export JAVA_HOME=$(brew --prefix)/opt/openjdk@$min_version/libexec/openjdk.jdk/Contents/Home"
      echo "  export PATH=\$JAVA_HOME/bin:\$PATH"
      export JAVA_HOME="$(brew --prefix)/opt/openjdk@$min_version/libexec/openjdk.jdk/Contents/Home"
      export PATH="$JAVA_HOME/bin:$PATH"
    else
      echo "[Java] ✗ 未安装 Homebrew，请手动安装 JDK $min_version"
      echo "  brew install openjdk@$min_version"
      return 1
    fi
  fi

  local java_version
  java_version=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+)\..*/\1/')
  if [ "$java_version" -lt "$min_version" ] 2>/dev/null; then
    echo "[Java] ✗ 当前 Java 版本: $java_version，需要 >= $min_version"
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
  else
    echo "[Java] ✓ Java $java_version"
  fi
}

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

  # AI: OPENAI_API_KEY / DEEPSEEK_API_KEY
  if [ -n "$OPENAI_API_KEY" ] || [ -n "$DEEPSEEK_API_KEY" ]; then
    local keys=""
    [ -n "$OPENAI_API_KEY" ] && keys="OPENAI_API_KEY"
    [ -n "$DEEPSEEK_API_KEY" ] && keys="${keys:+$keys, }DEEPSEEK_API_KEY"
    echo "[AI] ✓ 已配置: $keys"
    START_AI=true
  else
    echo "[AI] ✗ OPENAI_API_KEY 和 DEEPSEEK_API_KEY 均未设置，跳过 AI 模块"
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

start_ai_module() {
  IFS='|' read -r module_dir display_name port <<< "${AI_MODULE[0]}"
  start_module "$module_dir" "$display_name" "$port"
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

  # 收集所有模块名（含特殊模块）
  local all_modules=("${MODULES[@]}" "${AI_MODULE[@]}" "${STREAM_MODULE[@]}" "${SEATA_MODULES[@]}")

  # 第一阶段：通过 PID 文件停止（脚本自身启动的进程）
  for pid_file in "$PID_DIR"/*.pid; do
    [ -f "$pid_file" ] || continue
    local name=$(basename "$pid_file" .pid)
    local pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      printf '[%s] 停止中 (PID: %s) ...' "$name" "$pid"
      kill "$pid"
      for i in $(seq 1 10); do
        if ! kill -0 "$pid" 2>/dev/null; then break; fi
        sleep 1
      done
      if kill -0 "$pid" 2>/dev/null; then
        kill -9 "$pid" 2>/dev/null || true
      fi
      echo " 已停止"
    else
      echo "[$name] 未在运行"
    fi
    rm -f "$pid_file"
  done

  # 第二阶段：扫描并停止外部启动的项目模块进程（如 IDE、手动 java -jar 等）
  echo ""
  echo "扫描外部启动的模块进程..."
  local found_external=false
  for entry in "${all_modules[@]}"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    # 精确匹配: spring-boot:run 或 项目 jar，避免误杀
    # 注意：spring-boot:run 启动的子进程可能以 .jar 结尾，需要同时匹配
    local killed
    killed=$(pgrep -f "${module_dir}.*(spring-boot:run|\.jar)" 2>/dev/null || true)
    if [ -n "$killed" ]; then
      found_external=true
      echo "$killed" | xargs kill 2>/dev/null || true
      sleep 2
      # 确认已退出，未退出则强杀
      local remaining
      remaining=$(pgrep -f "${module_dir}.*(spring-boot:run|\.jar)" 2>/dev/null || true)
      if [ -n "$remaining" ]; then
        echo "$remaining" | xargs kill -9 2>/dev/null || true
      fi
      echo "[$display_name] 已停止外部进程 (PID: $(echo $killed | tr '\n' ' '))"
    fi
  done
  if ! $found_external; then
    echo "未发现外部启动的模块进程"
  fi

  # 停止 RocketMQ 和 Seata Server
  pkill -f "rocketmq" 2>/dev/null || true
  sleep 1
  pgrep -f "rocketmq" 2>/dev/null | xargs kill -9 2>/dev/null || true
  pkill -f "seata.*spring-boot:run" 2>/dev/null || true
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

  # Trace 链路追踪验证
  echo ""
  echo "========== Trace 链路追踪验证 =========="
  echo "  提示: 执行 bash .qoder/skills/demo-spring-cloud/verify-trace.sh 验证五条链路 trace 传播 (Web→Web / Web→gRPC / Web→Dubbo / Reactive→Reactive / Reactive→Dubbo)"
  echo "=================================="

  # Nacos Config 验证
  echo ""
  echo "========== Nacos Config 验证 =========="
  verify_url "http://localhost:8761/actuator/health" "Nacos Config 模块健康检查"
  echo "=================================="

  # AI 模块验证
  if [ -f "$PID_DIR/ai.pid" ] && kill -0 "$(cat "$PID_DIR/ai.pid")" 2>/dev/null; then
    echo ""
    echo "========== Spring AI 模块验证 =========="
    verify_url "http://localhost:8888/actuator/health" "AI 模块健康检查"
    echo "=================================="
  fi

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
    echo "     → 使用 demo-spring-cloud skill 执行 verify-trace.sh"
    echo ""
    echo "  2️⃣  Nacos Config 动态配置 (端口 8761):"
    echo "     • 配置发布/读取/删除"
    echo "     • @NacosConfig 注解注入与动态刷新"
    echo "     • @ConfigurationProperties + @Value + @RefreshScope"
    echo "     → 使用 demo-spring-cloud skill 执行 verify-nacos-config.sh"
    echo ""
    echo "  3️⃣  Sentinel 限流规则:"
    echo "     • Nacos 配置限流规则"
    echo "     • 验证限流效果"
    echo "     → 使用 demo-spring-cloud skill 执行 verify-sentinel-gateway.sh"
    echo ""
    echo "  4️⃣  Stream 消息收发 (RocketMQ):"
    echo "     • 基础消费: StreamBridge → topic → Consumer"
    echo "     • 定时消息源: Supplier 每秒自动发送"
    echo "     • 消息处理管道: Function 转换管道 (REST → toUpperCase → output)"
    echo "     • 延迟消息: StreamBridge + DELAY header 延迟投递"
    echo "     • 顺序消息: StreamBridge + ORDER_KEY 顺序消费"
    echo "     • 事务消息: StreamBridge + TransactionListener 两阶段提交"
    echo "     → 使用 demo-spring-cloud skill 执行 verify-stream.sh"
    echo ""
    echo "  5️⃣  Seata 分布式事务 (端口 18081-18084):"
    echo "     • 全局事务回滚/提交场景"
    echo "     • Feign 调用链 Xid 传递"
    echo "     • 数据一致性验证"
    echo "     → 使用 demo-spring-cloud skill 执行 verify-seata.sh"
    echo ""
    echo "  6️⃣  Spring AI 深度功能 (端口 8888):"
    echo "     • 聊天对话、流式输出、结构化提取"
    echo "     • Tool Calling、ReAct Agent"
    echo "     • 多模态视觉识别 (6种场景)"
    echo "     • DeepSeek 多提供商集成 (需配置 DEEPSEEK_API_KEY)"
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
  local all_special=("${AI_MODULE[0]}" "${STREAM_MODULE[0]}" "${SEATA_MODULES[@]}")
  for entry in "${all_special[@]}"; do
    IFS='|' read -r module_dir display_name port <<< "$entry"
    _check_status "$display_name" "$port"
  done
  echo "=============================="
}

# Nacos 注册中心地址
NACOS_HOST="127.0.0.1"
NACOS_PORT="8848"

check_nacos() {
  printf '[Nacos] 检查注册中心 (%s:%s) ...' "$NACOS_HOST" "$NACOS_PORT"
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
    printf '[Nacos] 等待启动就绪...'
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
    check_java
    echo ""
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
    check_java
    echo ""
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
  help|--help|-h)
    echo "用法: $0 {start|stop|restart|install|build|clean|verify|logs|status|help}"
    echo ""
    echo "命令说明:"
    echo "  start    启动所有服务（默认）"
    echo "  stop     停止所有服务（含 RocketMQ、Seata Server）"
    echo "  restart  重启所有服务"
    echo "  install  检查并安装中间件 + 打包模块"
    echo "  build    打包所有模块"
    echo "  clean    清理构建产物"
    echo "  verify   执行验证（不启动，仅验证已运行的服务）"
    echo "  status   查看服务状态"
    echo "  logs     查看模块日志 (用法: $0 logs <模块名>)"
    echo "  help     显示此帮助信息"
    ;;
  *)
    echo "用法: $0 {start|stop|restart|install|build|clean|verify|logs|status|help}"
    exit 1
    ;;
esac
