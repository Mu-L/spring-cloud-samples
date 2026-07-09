#!/bin/bash
#
# Spring Cloud Samples - 微服务 Docker 构建脚本
# 架构: 中间件本地安装，微服务 Docker 容器化
#
# 用法:
#   ./docker-build.sh build             # Maven 打包 + 构建所有 Docker 镜像
#   ./docker-build.sh build-one <module> # 构建指定模块 (如 cloud-provider-sample)
#   ./docker-build.sh up            # 启动核心微服务 (9个)
#   ./docker-build.sh up-stream     # 启动 Stream 消息模块
#   ./docker-build.sh up-ai         # 启动 Spring AI 模块 (2个)
#   ./docker-build.sh up-seata      # 启动 Seata 分布式事务 (7个)
#   ./docker-build.sh up-all        # 启动全部 (含 Stream/AI/Seata)
#   ./docker-build.sh down        # 停止所有微服务
#   ./docker-build.sh clean       # 停止并清除
#   ./docker-build.sh status      # 查看容器状态
#   ./docker-build.sh logs [svc]  # 查看日志
#
# 前置条件: 本地中间件已启动 (Nacos/RocketMQ/MySQL/PostgreSQL)
#

set -e

PROJECT="spring-cloud-samples"
IMAGE_PREFIX="${PROJECT}"

# 核心微服务模块
CORE_MODULES=(
  "cloud-nacos-discovery-sample"
  "cloud-nacos-config-sample"
  "cloud-provider-sample"
  "cloud-provider-reactive-sample"
  "cloud-provider-dubbo-sample"
  "cloud-grpc-server-sample"
  "cloud-consumer-sample"
  "cloud-consumer-reactive-sample"
  "cloud-gateway-sample"
)

# 可选模块
STREAM_MODULE="cloud-stream-sample"
AI_MODULES=("cloud-ai-sample" "cloud-ai-rag-sample")
SEATA_MODULES=(
  "cloud-seata-sample/account-dubbo-service"
  "cloud-seata-sample/account-service"
  "cloud-seata-sample/storage-dubbo-service"
  "cloud-seata-sample/storage-service"
  "cloud-seata-sample/order-dubbo-service"
  "cloud-seata-sample/order-service"
  "cloud-seata-sample/business-service"
)

# 模块名 → 镜像名
module_to_image() {
  echo "${IMAGE_PREFIX}/$(basename "$1")"
}

# Maven 打包
maven_build() {
  echo "========== Maven 打包 =========="
  ./mvnw clean package -DskipTests -q
  if [ $? -eq 0 ]; then
    echo "✓ Maven 打包成功"
  else
    echo "✗ Maven 打包失败"
    exit 1
  fi
}

# 构建单个 Docker 镜像
build_image() {
  local module="$1"
  local image_name
  image_name=$(module_to_image "$module")
  printf '[Docker] 构建 %-40s ...' "$image_name"
  docker build --build-arg MODULE="$module" -t "$image_name" . -q
  echo " ✓"
}

# 构建指定模块
build_one() {
  local module="$1"
  if [ -z "$module" ]; then
    echo "用法: $0 build-one <module-name>"
    echo "示例: $0 build-one cloud-provider-sample"
    echo "      $0 build-one cloud-seata-sample/business-service"
    exit 1
  fi
  echo "========== 构建指定模块: $module =========="
  echo ""
  echo "--- Maven 打包 ---"
  ./mvnw -pl "$module" clean package -DskipTests -q
  echo "✓ Maven 打包成功"
  echo ""
  echo "--- 构建 Docker 镜像 ---"
  build_image "$module"
  echo ""
  docker image prune -f
  echo ""
  echo "✓ 模块 $module 构建完成"
}

# 构建所有 Docker 镜像
build_all_images() {
  echo ""
  echo "========== 构建 Docker 镜像 =========="

  for module in "${CORE_MODULES[@]}"; do
    build_image "$module"
  done
  build_image "$STREAM_MODULE"
  for module in "${AI_MODULES[@]}"; do
    build_image "$module"
  done
  for module in "${SEATA_MODULES[@]}"; do
    build_image "$module"
  done

  # 清理旧镜像（删除悬空镜像，释放磁盘空间）
  echo ""
  echo "========== 清理旧镜像 =========="
  docker image prune -f

  echo "✓ 所有镜像构建完成"
}

# 启动核心微服务
up_core() {
  echo "========== 启动核心微服务 =========="
  docker compose --profile core up -d
  echo ""
  echo "✓ 核心微服务已启动"
  echo ""
  echo "========== 服务端口 =========="
  echo "  Gateway:          http://localhost:8764"
  echo "  Provider:         http://localhost:8765"
  echo "  Consumer:         http://localhost:8766"
  echo "  Nacos Discovery:  http://localhost:8760"
  echo "  Nacos Config:     http://localhost:8761"
  echo "  gRPC Server:      http://localhost:8090"
  echo ""
  echo "验证:"
  echo "  curl http://localhost:8766/hi?name=docker"
  echo "  curl http://localhost:8764/consumer-sample/hi?name=docker"
}

# 启动全部
up_all() {
  echo "========== 启动全部微服务 =========="
  docker compose --profile all up -d
  echo ""
  echo "✓ 全部微服务已启动"
  echo ""
  echo "========== 服务端口 =========="
  echo "  Gateway:          http://localhost:8764"
  echo "  Stream:           http://localhost:8767"
  echo "  AI:               http://localhost:8888"
  echo "  AI RAG:           http://localhost:8889"
  echo "  Seata Business:   http://localhost:18081"
}

# 启动 Stream 消息模块
up_stream() {
  echo "========== 启动 Stream 消息模块 =========="
  docker compose --profile stream up -d
  echo ""
  echo "✓ Stream 服务已启动"
  echo ""
  echo "========== 服务端口 =========="
  echo "  Stream:           http://localhost:8767"
}

# 启动 Spring AI 模块 (2个)
up_ai() {
  echo "========== 启动 Spring AI 模块 =========="
  docker compose --profile ai up -d
  echo ""
  echo "✓ AI 服务已启动 (2个模块)"
  echo ""
  echo "========== 服务端口 =========="
  echo "  AI:               http://localhost:8888"
  echo "  AI RAG:           http://localhost:8889"
}

# 启动 Seata 分布式事务 (7个模块)
up_seata() {
  echo "========== 启动 Seata 分布式事务 =========="
  docker compose --profile seata up -d
  echo ""
  echo "✓ Seata 服务已启动 (7个模块)"
  echo ""
  echo "========== 服务端口 =========="
  echo "  Business:         http://localhost:18081"
  echo "  Order:            http://localhost:18083"
  echo "  Storage:          http://localhost:18082"
  echo "  Account:          http://localhost:18084"
  echo "  Account Dubbo:    50071"
  echo "  Storage Dubbo:    50072"
  echo "  Order Dubbo:      50073"
  echo ""
  echo "验证:"
  echo "  curl http://localhost:18081/seata/rest"
  echo "  curl http://localhost:18081/seata/feign"
  echo "  curl http://localhost:18081/seata/dubbo"
}

# 停止所有
down_all() {
  echo "========== 停止所有微服务 =========="
  docker compose --profile all down
  echo "✓ 所有微服务已停止"
}

# 停止并清除
clean_all() {
  echo "========== 停止并清除 =========="
  docker compose --profile all down
  docker system prune -f
  echo "✓ 已清理"
}

# 查看状态
status() {
  docker compose --profile all ps
}

# 查看日志
logs() {
  local service="$1"
  if [ -z "$service" ]; then
    docker compose --profile all logs -f
  else
    docker compose logs -f "$service"
  fi
}

# 主逻辑
case "${1:-help}" in
  build)
    maven_build
    build_all_images
    ;;
  build-one)
    build_one "$2"
    ;;
  up)
    up_core
    ;;
  up-stream)
    up_stream
    ;;
  up-ai)
    up_ai
    ;;
  up-seata)
    up_seata
    ;;
  up-all)
    up_all
    ;;
  down)
    down_all
    ;;
  clean)
    clean_all
    ;;
  status)
    status
    ;;
  logs)
    logs "$2"
    ;;
  help|*)
    echo "Spring Cloud Samples - 微服务 Docker 部署"
    echo ""
    echo "架构: 中间件本地安装 + 微服务 Docker 容器化"
    echo ""
    echo "用法: $0 <command>"
    echo ""
    echo "命令:"
    echo "  build             Maven 打包 + 构建所有 Docker 镜像"
    echo "  build-one <mod>   构建指定模块 (如 cloud-provider-sample)"
    echo "  up          启动核心微服务 (9个)"
    echo "  up-stream   启动 Stream 消息模块"
    echo "  up-ai       启动 Spring AI 模块 (2个)"
    echo "  up-seata    启动 Seata 分布式事务 (7个)"
    echo "  up-all      启动全部 (含 Stream/AI/Seata)"
    echo "  down        停止所有微服务"
    echo "  clean       停止并清除"
    echo "  status      查看容器状态"
    echo "  logs [svc]  查看日志"
    echo ""
    echo "快速开始:"
    echo "  1. 确保本地中间件已启动 (Nacos/RocketMQ/MySQL/PostgreSQL)"
    echo "  2. $0 build    # 首次构建"
    echo "  3. $0 up       # 启动核心服务"
    echo "  4. $0 down     # 停止"
    ;;
esac
