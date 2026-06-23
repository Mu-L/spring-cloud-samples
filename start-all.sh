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

# 模块列表: 目录名 | 显示名称 | 端口
MODULES=(
  "cloud-provider-dubbo-sample|provider-dubbo|-"
  "cloud-consumer-dubbo-sample|consumer-dubbo|-"
  "cloud-provider-reactive-sample|provider-reactive|8762"
  "cloud-consumer-reactive-sample|consumer-reactive|8763"
  "cloud-gateway-sample|gateway|8764"
  "cloud-provider-sample|provider|8765"
  "cloud-consumer-sample|consumer|8766"
  "cloud-grpc-server-sample|grpc-server|8090"
  "cloud-grpc-client-sample|grpc-client|-"
  "cloud-nacos-config-sample|nacos-config|8761"
)

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

  # Nacos Config 配置中心验证（优先验证，失败则停止后续验证）
  echo ""
  echo "========== Nacos Config 验证 =========="
  echo "[Nacos Config] 发布配置: dataId=my.city, content=wuhan"
  local pub_resp
  pub_resp=$(curl -s -w '\n  HTTP Status: %{http_code}' --max-time 10 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan' 2>/dev/null)
  echo "  响应: $pub_resp"
  echo "[Nacos Config] 读取配置: dataId=my.city"
  local get_resp
  get_resp=$(curl -s -w '\n  HTTP Status: %{http_code}' --max-time 10 'http://localhost:8761/nacos/getConfig?dataId=my.city' 2>/dev/null)
  echo "  响应: $get_resp"
  if echo "$get_resp" | grep -q "wuhan" 2>/dev/null; then
    echo "[Nacos Config] 验证成功! 配置读写正常"
    VERIFY_PASS=$((VERIFY_PASS + 1))
  else
    echo "[Nacos Config] 验证失败，请查看日志: $LOG_DIR/nacos-config.log"
    echo "[Nacos Config] 停止后续验证..."
    VERIFY_FAIL=$((VERIFY_FAIL + 1))
    VERIFY_FAILED_LIST+=("[Nacos Config] 配置读写验证失败")
    echo "=================================="
    # 汇总验证结果
    echo ""
    echo "=========================================="
    echo "  验证结果汇总: 通过 $VERIFY_PASS 项, 失败 $VERIFY_FAIL 项"
    echo "=========================================="
    echo ""
    echo "  以下验证项失败:"
    for failed in "${VERIFY_FAILED_LIST[@]}"; do
      echo "    - $failed"
    done
    echo ""
    return 1
  fi
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
  echo "=============================="
}

# Nacos 注册中心地址
NACOS_HOST="127.0.0.1"
NACOS_PORT="8848"

check_nacos() {
  echo -n "[Nacos] 检查注册中心 ($NACOS_HOST:$NACOS_PORT) ..."
  for i in $(seq 1 15); do
    if curl -s -o /dev/null -w '' "http://$NACOS_HOST:$NACOS_PORT/nacos/actuator/health" 2>/dev/null; then
      echo " 就绪"
      return 0
    fi
    sleep 2
  done
  echo " 未就绪!"
  echo "请先启动 Nacos (http://$NACOS_HOST:$NACOS_PORT/nacos)，再运行本脚本。"
  exit 1
}

install_api() {
  echo "正在安装 cloud-sample-api 到本地仓库 ..."
  cd "$BASE_DIR"
  # 先安装根 pom，再安装 api 模块（api 依赖父 pom）
  if ./mvnw -N install -q && ./mvnw -pl cloud-sample-api install -DskipTests -q; then
    echo "cloud-sample-api 安装成功"
  else
    echo "cloud-sample-api 安装失败!"
    exit 1
  fi
}

# 主逻辑
case "${1:-start}" in
  start)
    echo "========== 启动所有服务 =========="
    check_nacos
    echo ""
    install_api
    echo ""
    for entry in "${MODULES[@]}"; do
      IFS='|' read -r module_dir display_name port <<< "$entry"
      start_module "$module_dir" "$display_name" "$port"
    done
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
    install_api
    echo ""
    for entry in "${MODULES[@]}"; do
      IFS='|' read -r module_dir display_name port <<< "$entry"
      start_module "$module_dir" "$display_name" "$port"
    done
    echo ""
    status_all
    demo_urls
    ;;
  status)
    status_all
    ;;
  *)
    echo "用法: $0 {start|stop|restart|status}"
    exit 1
    ;;
esac
