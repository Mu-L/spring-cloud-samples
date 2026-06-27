---
name: demo-spring-cloud
description: >
  启动和演示 Spring Cloud Alibaba 示例项目的各微服务模块。当用户要求演示项目、启动服务、
  验证微服务调用、测试网关路由、查看服务注册、执行集成测试、一键部署、环境检查、
  排查微服务问题、了解 Spring Cloud 组件用法、学习 Nacos/Sentinel/Seata/Dubbo/gRPC/Stream 时
  使用此技能。涵盖 16 个模块的完整演示流程。
tags: [spring-cloud, spring-cloud-alibaba, nacos, sentinel, seata, dubbo, grpc, rocketmq, stream, microservices, demo]
---

# Spring Cloud Alibaba 示例项目演示

## ⚠️ 重要说明

**所有验证操作必须严格按照本 SKILL 的要求执行，特别是：**

1. **Nacos 配置管理**：读写 Nacos 配置时，**必须使用 `cloud-nacos-config-sample` 模块提供的接口**（端口 8761），不要直接使用 Nacos 官方 HTTP API。
   - ✅ 正确方式：`http://localhost:8761/nacos/publishConfig`、`http://localhost:8761/nacos/getConfig`
   - ❌ 错误方式：`http://localhost:8080/nacos/v1/cs/configs`

2. **验证流程**：按照 SKILL 中定义的步骤顺序执行，不要跳过任何前置检查或验证环节。

3. **端口规范**：项目已统一端口分配，AI 模块使用 8888 端口，禁止使用 8080 端口。

---

## 30 秒快速体验

只需两步：
1. 确保 Nacos 已运行（没有？告诉 AI "安装 Nacos"）
2. 告诉 AI **"演示本项目"**

AI 会自动完成：环境检查 → 依赖安装 → 服务启动 → 接口验证 → 结果汇总。无需手动操作。

> 也可以只验证单个模块，例如："验证 Seata 分布式事务"、"验证 Stream 消息收发"、"演示 Spring AI"

### 验证层级说明

**基础验证**（`start-all.sh start` 自动执行）：
- ✓ 服务注册发现
- ✓ 健康检查
- ✓ 基础调用链路（Web/Reactive/Dubbo/gRPC）
- ✓ 网关路由
- ✓ Nacos Config 发布/获取
- ✓ AI/Stream/Seata 模块健康检查

**深度验证**（需要单独执行）：
- 🔍 Sentinel 限流：规则配置、限流效果验证
- 🔍 Stream 消息消费：实际消费逻辑、多 Topic 处理、Consumer Group 行为
- 🔍 Seata 分布式事务：全局事务回滚/提交、Xid 传递、数据一致性
- 🔍 Spring AI 深度功能：聊天对话、流式输出、Tool Calling、ReAct Agent、多模态视觉识别

> 使用 `demo-spring-cloud` skill 可执行深度验证脚本，或查看各模块 README 了解详细用法

## 项目概述

基于 **Spring Boot 4.x + Spring Cloud Alibaba 2025.1.x** 的生产级微服务示例项目，包含 16 个模块。

## 前置条件

### 1. Nacos 注册中心（必须）

所有模块依赖 Nacos，启动前先确认 Nacos 已就绪。

**当用户说"安装 Nacos"时，按以下流程执行：**

**Step 1：检查 Nacos 状态**
```bash
curl -s http://127.0.0.1:8848/nacos/actuator/health | grep -q '"status":"UP"' && echo "✓ Nacos 已运行" || echo "✗ Nacos 未运行"
```

根据检查结果，进入对应场景：

---

#### 场景 A：Nacos 已运行 ✓

直接跳到 [Step 4：设置环境变量](#step-4设置环境变量)。

---

#### 场景 B：Nacos 已安装但未运行

查找已安装的 Nacos 目录：

macOS / Linux：
```bash
NACOS_DIR=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
echo "找到 Nacos: $NACOS_DIR"
```
Windows（PowerShell）：
```powershell
$NACOS_DIR = Get-ChildItem "$env:USERPROFILE" -Directory -Filter "nacos-*" | Sort-Object Name | Select-Object -Last 1
Write-Host "找到 Nacos: $NACOS_DIR"
```

启动 Nacos：

macOS / Linux：
```bash
cd "$NACOS_DIR"
bin/startup.sh -m standalone
```
Windows（CMD）：
```cmd
cd %NACOS_DIR%
bin\startup.cmd -m standalone
```

等待启动完成后，跳到 [Step 4：设置环境变量](#step-4设置环境变量)。

---

#### 场景 C：Nacos 未安装

执行以下安装流程：

**前置检查：JDK**

Nacos Server 是 Java 应用，需要 JDK 17+（推荐 JDK 21）。
```bash
java -version 2>&1 | head -1
```
若未安装或版本过低：

macOS：
```bash
brew install openjdk@21
sudo ln -sfn $(brew --prefix openjdk@21)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```
Windows：
下载并安装 [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 或 [Adoptium JDK](https://adoptium.net/)，确保 `JAVA_HOME` 环境变量已配置。

Linux（Ubuntu/Debian）：
```bash
sudo apt install -y openjdk-21-jdk
```

**Step 3：安装并部署**

> Nacos 支持两种存储模式：**内嵌数据库**（Derby，零配置，适合快速体验）和 **MySQL**（推荐，数据持久化，适合开发/生产）。
> 询问用户选择哪种模式。若用户未明确，推荐 MySQL 模式。

#### Step 3-0：下载 Nacos Server 并安装 CLI

Nacos 提供两种安装方式：

**方式一：一键安装器（推荐，自动安装 Server + CLI）**

macOS / Linux：
```bash
curl -fsSL https://nacos.io/nacos-installer.sh | bash
```
> 一键安装器会自动完成：下载 Nacos Server → 安装 Nacos CLI 工具 → 配置 PATH。
> 安装后可直接使用 `nacos`、`nacos-setup` 等 CLI 命令。

Windows：
1. 访问 https://github.com/alibaba/nacos/releases
2. 下载 `nacos-server-*.zip`
3. 解压到任意目录（如 `%USERPROFILE%\nacos`）
4. 将 `%USERPROFILE%\nacos\nacos\bin` 添加到系统 PATH（可选，方便直接使用 CLI 命令）

**方式二：手动下载二进制包（无 CLI，需手动操作）**

macOS / Linux：
```bash
cd "$HOME"
curl -LO https://github.com/alibaba/nacos/releases/latest/download/nacos-server-*.tar.gz
tar -xzf nacos-server-*.tar.gz
NACOS_DIR=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
```

##### Nacos CLI 工具说明

###### 服务管理
| 命令 | 说明 |
|------|------|
| `nacos-setup` | 一键部署单机实例（初始化内置数据库、创建默认账号） |
| `nacos version` | 查看已安装的 Nacos 版本 |

###### 配置管理（`nacos config`）
| 命令 | 说明 |
|------|------|
| `nacos config publish -d <dataId> -g <group> -c <content>` | 发布配置 |
| `nacos config get -d <dataId> -g <group>` | 获取配置 |
| `nacos config delete -d <dataId> -g <group>` | 删除配置 |
| `nacos config list -g <group>` | 列出指定 group 下的所有配置 |

###### 命名空间管理（`nacos namespace`）
| 命令 | 说明 |
|------|------|
| `nacos namespace create -n <name>` | 创建命名空间 |
| `nacos namespace list` | 列出所有命名空间 |

使用 `nacos config --help` 或 `nacos namespace --help` 查看完整参数。

#### 模式 A：内嵌数据库（快速体验）

macOS / Linux（若已通过一键安装器安装）：
```bash
nacos-setup
```
> `nacos-setup` 会自动部署单机实例并创建密码（用户名：nacos），该密码写入内置数据库。

macOS / Linux（若手动下载了二进制包）：
```bash
cd "$NACOS_DIR"
bin/startup.sh -m standalone
```

Windows：
```cmd
cd %USERPROFILE%\nacos\nacos\bin
startup.cmd -m standalone
```
首次启动后，访问 http://127.0.0.1:8080/ ，使用默认账号 `nacos/nacos` 登录并修改密码。

#### 模式 B：MySQL（推荐）

前置条件：MySQL 已安装并运行。

**Step 3a：创建 Nacos 数据库**
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS nacos DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

**Step 3b：初始化 Nacos 表结构**

从 Nacos 发行包中获取 SQL 脚本，或从官方仓库下载：
```bash
# 若已通过一键安装器安装，SQL 文件位于 nacos 目录下的 conf/mysql-schema.sql
NACOS_DIR=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
mysql -u root -p nacos < "$NACOS_DIR/conf/mysql-schema.sql"
```
若未找到本地 SQL 文件，从官方仓库获取（替换为对应版本号）：
```bash
curl -fsSL "https://raw.githubusercontent.com/alibaba/nacos/develop/distribution/conf/mysql-schema.sql" -o /tmp/nacos-mysql-schema.sql
mysql -u root -p nacos < /tmp/nacos-mysql-schema.sql
```

**Step 3c：配置 Nacos 使用 MySQL**

编辑 `conf/application.properties`，添加/修改以下内容：
```properties
spring.datasource.platform=mysql
db.num=1
db.url.0=jdbc:mysql://127.0.0.1:3306/nacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
db.user.0=root
db.password.0=<MySQL密码>
```

**Step 3d：启动 Nacos**

macOS / Linux：
```bash
cd "$NACOS_DIR"
bin/startup.sh -m standalone
```
Windows（CMD）：
```cmd
cd %NACOS_DIR%
bin\startup.cmd -m standalone
```

> **Nacos Server 自身地址配置**：
>
> 默认情况下 Nacos Server 会自动获取本机 IP 并监听 `0.0.0.0:8848`，本地开发无需额外配置。
> 以下场景需要手动指定：
>
> **多网卡 / Docker 环境**：启动时指定 IP
> ```bash
> # macOS / Linux
> bin/startup.sh -m standalone --spring.cloud.nacos.server-ip=<本机IP>
> # Windows
> bin\startup.cmd -m standalone --spring.cloud.nacos.server-ip=<本机IP>
> ```
> 或在 `conf/application.properties` 中配置：
> ```properties
> nacos.server.ip=<本机IP>
> ```
>
> **修改端口**（默认 8848）：
> ```properties
> server.port=8848
> nacos.server.port=8848
> ```
>
> **客户端连接地址**（本项目各模块默认值，通常无需修改）：
> ```yaml
> spring:
>   cloud:
>     nacos:
>       server-addr: 127.0.0.1:8848  # 本地开发默认值
> ```

**Step 3e：初始化账号**
首次启动后，访问 http://127.0.0.1:8080/ ，使用默认账号 `nacos/nacos` 登录并修改密码。

**Step 3f：Nacos Console（Web 控制台）**

登录后可通过浏览器访问 Nacos Console，主要功能：

| 功能 | 路径 | 说明 |
|------|------|------|
| 配置管理 | 配置管理 → 配置列表 | 发布、编辑、删除、搜索配置，查看配置历史版本和差异 |
| 服务管理 | 服务管理 → 服务列表 | 查看已注册的服务、实例列表、元数据、健康状态 |
| 命名空间 | 命名空间 | 创建/管理命名空间，实现环境隔离（dev/test/prod） |
| 权限控制 | 权限控制 → 用户列表 / 角色列表 | 管理用户、角色和权限 |

> 本项目的 Sentinel 限流规则、Seata 配置等均可通过 Console 的配置管理界面发布和管理，
> 比 CLI 或 curl 更直观。

**Step 4：设置环境变量**
提示用户设置环境变量（用户名/密码为安装时创建的凭证）：

macOS / Linux：
```bash
export SPRING_CLOUD_NACOS_USERNAME=nacos
export SPRING_CLOUD_NACOS_PASSWORD=<安装时设置的密码>
```
Windows（CMD）：
```cmd
set SPRING_CLOUD_NACOS_USERNAME=nacos
set SPRING_CLOUD_NACOS_PASSWORD=<安装时设置的密码>
```
Windows（PowerShell）：
```powershell
$env:SPRING_CLOUD_NACOS_USERNAME = "nacos"
$env:SPRING_CLOUD_NACOS_PASSWORD = "<安装时设置的密码>"
```

**Step 5：验证**
等待 Nacos 启动完成后再次检查健康状态，确认 `"status":"UP"` 后告知用户 Nacos 已就绪。

**停止 Nacos**：

macOS / Linux：
```bash
NACOS_DIR=$(find "$HOME" -maxdepth 1 -type d -name 'nacos-*' | sort -V | tail -1)
cd "$NACOS_DIR"
bin/shutdown.sh
```
Windows（CMD）：
```cmd
cd %NACOS_DIR%\nacos\bin
shutdown.cmd
```

### 2. RocketMQ（仅 Stream 模块需要）

Stream 模块依赖 RocketMQ，询问用户是否需要帮助安装和启动。 <br>

**RocketMQ 检查**：
```bash
nc -z 127.0.0.1 9876 && echo "✓ RocketMQ NameServer 已运行" || echo "✗ RocketMQ 未运行"
```

若未安装：
```bash
curl -O https://dist.apache.org/repos/dist/release/rocketmq/5.5.0/rocketmq-all-5.5.0-bin-release.zip
unzip rocketmq-all-5.5.0-bin-release.zip -d $HOME
```

启动步骤：
```bash
ROCKETMQ_HOME=$(find "$HOME" -maxdepth 1 -type d -name 'rocketmq-*' | sort -V | tail -1)
cd "$ROCKETMQ_HOME"

# 启动 NameServer
nohup bin/mqnamesrv > namesrv.log 2>&1 &
sleep 5

# 启动 Broker
nohup bin/mqbroker -n localhost:9876 > broker.log 2>&1 &
sleep 10

# 验证
nc -z 127.0.0.1 9876 && echo "✓ NameServer 已启动" || echo "✗ NameServer 启动失败"
nc -z 127.0.0.1 10911 && echo "✓ Broker 已启动" || echo "✗ Broker 启动失败"
```

### 3. MySQL + Seata Server（仅 Seata 模块需要）

Seata 分布式事务模块依赖 MySQL 和 Seata Server：

**MySQL 检查**：
```bash
mysql -u root -proot1234 -e "SELECT 1"
```

若 MySQL 未安装或密码不对：
```bash
# 安装并启动（macOS）
brew install mysql
mysql.server start
mysqladmin -u root password 'root1234'
```

> 项目统一使用 root/root1234，若已有 MySQL 且密码不同，请重置密码或自行修改各模块 application.yml 中的数据库配置。

**数据库初始化**：
```bash
mysql -u root -proot1234 -e "CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -proot1234 seata < cloud-seata-sample/all.sql
```

**Seata Server 检查**（端口 8091）：
```bash
nc -z 127.0.0.1 8091 && echo "✓ Seata Server 已运行" || echo "✗ Seata Server 未运行"
```

若 Seata Server 未运行，需从源码构建启动：

**Seata Server 源码启动方式**：

```bash
# 版本：2.8.0-SNAPSHOT（已预置 Nacos 配置，无需修改 application.yml）

# 1. 查找或克隆 Seata 源码
SEATA_SRC="$HOME/github/seata"
if [ ! -d "$SEATA_SRC" ]; then
  echo "Seata 源码不存在，正在克隆..."
  mkdir -p "$HOME/github"
  git clone https://github.com/javahongxi/seata.git "$SEATA_SRC"
fi

# 2. 构建（首次或代码更新时需要）
cd "$SEATA_SRC"
./mvnw clean install -DskipTests -q

# 3. 启动 Seata Server（非 fat jar，需用 mvnw spring-boot:run）
cd "$SEATA_SRC"
nohup ./mvnw -pl server spring-boot:run > /tmp/seata-server.log 2>&1 &
echo "Seata Server 启动中..."

# 4. 等待启动完成（检查端口 8091）
for i in $(seq 1 30); do
  if nc -z 127.0.0.1 8091 2>/dev/null; then
    echo "✓ Seata Server 已启动 (端口 8091)"
    break
  fi
  sleep 1
done
```

**为什么需要从源码启动**：
- 项目使用的 Nacos 版本与 Seata 官方发布的二进制包存在兼容性问题
- 该克隆版本已修复兼容性问题并预置了 Nacos 配置

### 4. 安装依赖模块

部分模块依赖 `cloud-commons` 和 `cloud-sample-api`，启动前需先安装：
```bash
./mvnw -N install -q && ./mvnw -pl cloud-commons,cloud-sample-api install -DskipTests -q
```

## 启动方式

> **优先级：AI Skill > 脚本 > 手动**

| 方式 | 说明 | 适用场景 |
|------|------|----------|
| 🤖 **AI Skill（推荐）** | 告诉 AI "演示项目"，自动完成环境检查、启动、验证全流程 | 快速体验、集成测试 |
| 📜 **一键脚本** | 通过 `start-all.sh` 自动化启动和验证 | 批量验证、CI/CD |
| 🔧 **手动启动** | 逐个模块手动启动，灵活控制 | 学习调试、单模块开发 |

### 方式一：AI Skill（推荐）

直接告诉 AI 助手你要做什么，例如：
- "演示项目"
- "启动所有服务并验证"
- "验证 Seata 分布式事务"
- "验证 Stream 消息收发"

AI 会自动检查环境、安装依赖、启动服务、执行验证。无需手动操作。

### 方式二：一键脚本

前置条件参考上方「前置条件」章节，脚本会自动检查并尝试启动缺失的组件。

```bash
sh start-all.sh install  # 检查并安装中间件 + 打包模块
sh start-all.sh          # 启动所有服务（自动检查前置条件、打包、启动、验证）
sh start-all.sh build    # 打包所有模块
sh start-all.sh verify   # 执行验证（不启动，仅验证已运行的服务）
sh start-all.sh status   # 查看服务状态
sh start-all.sh logs <模块名>  # 查看模块日志（如 ai, stream, provider）
sh start-all.sh stop     # 停止所有服务（含 RocketMQ、Seata Server）
sh start-all.sh restart  # 重启所有服务
sh start-all.sh clean    # 清理构建产物
```

> 脚本流程：检查 Nacos → 检查 RocketMQ/MySQL/Seata Server（自动启动）→ 安装依赖模块 → 打包 → 按顺序启动所有模块 → 执行验证 → 汇总结果

### 方式三：手动逐个启动

前置条件参考上方「前置条件」章节。

启动顺序原则：**基础设施 → Provider → Consumer → Client → Config**

| 顺序 | 模块 | 端口 | 说明 |
|------|------|------|------|
| 1 | cloud-nacos-discovery-sample | 8760 | 服务发现 |
| 2 | cloud-gateway-sample | 8764 | 网关 |
| 3 | cloud-provider-sample | 8765 | Web Provider |
| 4 | cloud-provider-reactive-sample | 8762 | Reactive Provider |
| 5 | cloud-provider-dubbo-sample | 50051 | Dubbo Provider |
| 6 | cloud-grpc-server-sample | 8090(Web)/9090(gRPC) | gRPC Server |
| 7 | cloud-consumer-sample | 8766 | Web Consumer |
| 8 | cloud-consumer-reactive-sample | 8763 | Reactive Consumer |
| 9 | cloud-consumer-dubbo-sample | - | Dubbo Consumer |
| 10 | cloud-grpc-client-sample | - | gRPC Client |
| 11 | cloud-nacos-config-sample | 8761 | Nacos Config |

单独启动某模块：
```bash
./mvnw -pl <模块目录> spring-boot:run
```

### 独立模块（无启动顺序依赖）

| 模块 | 端口 | 说明 |
|------|------|------|
| cloud-ai-sample | 8888 | Spring AI，需配置 OPENAI_API_KEY |
| cloud-stream-sample | - | 需先安装并启动 RocketMQ |
| cloud-seata-sample | 18081-18084 | 需 MySQL + Seata Server |

## 演示与验证

### 1. Nacos Discovery 服务发现

```bash
curl http://localhost:8760/discovery/instances
```

### 2. 普通 Web 服务调用

```bash
# 直接访问 (consumer → provider)
curl 'http://localhost:8766/hi?name=hongxi'
# 通过网关 (gateway → consumer → provider)
curl 'http://localhost:8764/consumer-sample/hi?name=hongxi'
```

### 3. Reactive Web 服务调用

```bash
# 直接访问 (consumer-reactive → provider-reactive)
curl 'http://localhost:8763/hi?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-reactive-sample/hi?name=hongxi'
```

### 4. Dubbo 服务调用

```bash
# consumer → provider-dubbo
curl 'http://localhost:8766/dubbo?name=hongxi'
# consumer-reactive → provider-dubbo
curl 'http://localhost:8763/dubbo?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-sample/dubbo?name=hongxi'
curl 'http://localhost:8764/consumer-reactive-sample/dubbo?name=hongxi'
```

### 5. gRPC 服务调用

```bash
# consumer → grpc-server
curl 'http://localhost:8766/grpc?name=hongxi'
# 通过网关
curl 'http://localhost:8764/consumer-sample/grpc?name=hongxi'
```

### 6. Dubbo REST 接口

```bash
# 直接访问 provider-dubbo
curl http://localhost:50051/api/hello/lily
curl 'http://localhost:50051/api/add?a=1&b=2'
curl -X POST http://localhost:50051/api/echo -H "Content-Type: application/json" -d '{"message":"hi"}'
curl 'http://localhost:50051/api/greet/lily?lang=zh'
# 通过网关
curl http://localhost:8764/provider-dubbo-sample/api/hello/lily
```

### 7. 纯 Dubbo / gRPC 演示

启动后观察日志即可：
- `cloud-consumer-dubbo-sample`：日志中出现 `Hello, lily` 表示调用成功
- `cloud-grpc-client-sample`：日志中出现 `Hello, lily` 表示调用成功

### 8. Nacos Config 动态配置

```bash
# 发布配置
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
# 获取配置
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
```

### 9. Sentinel 网关限流

**前提**：`cloud-gateway-sample`（8764）、`cloud-consumer-sample`（8766）、`cloud-provider-sample`（8765）、`cloud-nacos-config-sample`（8761）已启动。

**执行一键验证脚本：**
```bash
bash .qoder/skills/demo-spring-cloud/verify-sentinel-gateway.sh
```
脚本会自动完成：检查前置服务 → 发布 gw-api-group/gw-flow 配置到 SENTINEL_GROUP → 验证配置写入 → 触发限流（10 次请求，consumer_api 阈值 5 QPS）→ 清理 Sentinel 配置

配置 JSON 参考项目 README 的 [Sentinel Gateway 演示](../../../README.md#-sentinel-gateway-演示) 章节。

### 10. Stream 消息收发（需 RocketMQ）

**执行流程：**

1. **检查 RocketMQ 并询问用户**：
   ```bash
   nc -z 127.0.0.1 9876 2>/dev/null && echo "✓ RocketMQ 已运行" || echo "✗ RocketMQ 未运行"
   ```

2. **询问用户是否需要 AI 自动完成环境准备**：
   > "Stream 模块依赖 RocketMQ，但当前未检测到运行中的 RocketMQ 服务。是否需要我帮您自动完成以下操作？
   > 1. 启动 NameServer 和 Broker
   > 2. 创建所需的 Topic 和 Consumer Group
   > 3. 启动 Stream 模块并验证消息收发"

   **如果用户同意**，直接执行一键验证脚本：
   ```bash
   bash .qoder/skills/demo-spring-cloud/verify-stream.sh
   ```
   脚本会自动完成：清理环境 → 检查 Nacos → 启动 RocketMQ（如未运行）→ 创建 Topic/ConsumerGroup → 打包 → 启动 Stream 模块 → 验证消息收发（stream-demo-topic + stream-demo-topic2）

---

**如果用户选择手动操作**，详细步骤请参考项目 README 的 [Stream 演示](../../../README.md#-stream-演示) 章节。

### 11. Seata 分布式事务（需 MySQL + Seata Server）

**执行流程：**

1. **检查前置条件**：
   ```bash
   # 检查 Nacos 是否运行
   if curl -s -o /dev/null -w '' "http://127.0.0.1:8848/nacos/actuator/health" 2>/dev/null; then
     echo "✓ Nacos 已运行"
   else
     echo "✗ Nacos 未运行，请先启动 Nacos"
     return 1
   fi

   # 检查 MySQL 是否运行
   if mysql -u root -proot1234 -e "SELECT 1" &>/dev/null; then
     echo "✓ MySQL 已运行"
   else
     echo "✗ MySQL 未运行或连接失败"
     return 1
   fi
   ```

2. **询问用户是否需要 AI 自动完成环境准备**：
   > "Seata 分布式事务示例需要以下环境：
   > 1. MySQL 数据库（已检测到运行中 / 未检测到）
   > 2. 初始化 seata 数据库及业务表
   > 3. 配置 Nacos（创建 seata.properties）
   > 4. 启动 Seata Server
   > 5. 启动 4 个微服务并验证分布式事务
   > 
   > 是否需要我帮您自动完成以上操作？"

   **如果用户同意**，直接执行一键验证脚本：
   ```bash
   bash .qoder/skills/demo-spring-cloud/verify-seata.sh
   ```
   脚本会自动完成：清理环境 → 检查前置条件 → 初始化数据库 → 打包 → 启动辅助服务 → 发布 Nacos 配置 → 启动 Seata Server → 并行启动 4 个微服务 → 验证分布式事务（回滚 + 提交 + Feign + Xid + 数据一致性）

---

**如果用户选择手动操作**，详细步骤请参考`seata-sample`模块的 README。

### 12. Spring AI 模块

启动前配置 API Key：
```bash
export OPENAI_API_KEY=your-api-key-here
```

启动 AI 模块（端口 8888）：
```bash
# 默认使用 qwen3.7-plus 模型（支持多模态视觉识别）
./mvnw -pl cloud-ai-sample spring-boot:run

# 如需切换其他模型，可通过命令行参数覆盖
./mvnw -pl cloud-ai-sample spring-boot:run -Dspring-boot.run.arguments=--spring.ai.openai.chat.options.model=<模型名>
```

等待 AI 模块就绪（通过 actuator 健康检查）：
```bash
for i in $(seq 1 60); do
  resp=$(curl -s "http://localhost:8888/actuator/health" 2>/dev/null)
  if echo "$resp" | grep -q '"status":"UP"'; then
    echo "AI 模块已就绪 (耗时 ${i}s)"
    break
  fi
  sleep 1
done
```

常用演示接口（中文参数需 URL 编码，使用 `--get --data-urlencode`）：
```bash
# 简单聊天
curl --get --data-urlencode "message=你好" "http://localhost:8888/ai/chat"
# 流式输出
curl --get --data-urlencode "message=讲一个故事" "http://localhost:8888/ai/chat/stream"
# 结构化输出
curl --get --data-urlencode "text=张三今年25岁，是软件工程师" "http://localhost:8888/ai/extract"
# Tool Calling
curl --get --data-urlencode "question=北京今天天气怎么样？" "http://localhost:8888/ai/tool/weather"
# ReAct Agent
curl --get --data-urlencode "question=北京天气怎么样？适合出门吗？" "http://localhost:8888/ai/agent/chat"
# MCP Server（SSE 端点）
# 连接地址: http://localhost:8888/sse
```

#### 多模态视觉识别

**⚠️ 重要说明：**
1. **默认模型 `qwen3.7-plus` 已支持视觉识别**，无需重启或切换模型。
2. **必须使用 SKILL 中提供的真实图片 URL**，不要自行构造不存在的图片地址。

**⚠️ 验证规范：**
- ✅ **正确做法**：严格按照下方示例中的 URL 进行测试，这些 URL 已验证可稳定访问。
- ❌ **错误做法**：自行构造图片 URL（如 `https://example.com/image.jpg`），会导致请求失败。

```bash
# 通过 URL 分析图片（神舟十号海报）
curl -X POST "http://localhost:8888/ai/vision/analyze-url" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg"

# 图片上传分析（项目根目录下的架构图）
curl -X POST "http://localhost:8888/ai/vision/analyze-upload" \
  -F "file=@arch.png"

# OCR 文字识别
curl -X POST "http://localhost:8888/ai/vision/ocr" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg"

# 图表分析（QuickChart.io 生成的柱状图）
curl -X POST "http://localhost:8888/ai/vision/chart-analysis" \
  -d "imageUrl=https://quickchart.io/chart?c=%7Btype%3A%27bar%27%2Cdata%3A%7Blabels%3A%5B%27Q1%27%2C%27Q2%27%2C%27Q3%27%2C%27Q4%27%5D%2Cdatasets%3A%5B%7Blabel%3A%27Revenue%27%2Cdata%3A%5B100%2C200%2C150%2C300%5D%7D%5D%7D%7D"

# 代码截图转代码（CSDN C语言代码图片）
curl -X POST "http://localhost:8888/ai/vision/code-from-image" \
  -d "imageUrl=https://i-blog.csdnimg.cn/blog_migrate/486ded85cb954f0da650e7f9c306900e.png"

# 多图片对比分析（神舟十号海报 vs 北京申奥号外）
curl -X POST "http://localhost:8888/ai/vision/compare" \
  -d "imageUrl1=https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg" \
  -d "imageUrl2=https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg"
```

> **⚠️ 再次强调：**
> - 以上所有 URL 都是**真实存在且已验证可访问**的图片地址，请直接使用。
> - **不要自行构造图片 URL**（如 `https://example.com/image.jpg`），这些地址不存在，会导致请求失败。
> - 如果视觉识别返回 500 错误，请检查：
>   1. 图片 URL 是否正确（必须使用上方示例中的 URL）
>   2. AI 模块是否正常运行（检查 `http://localhost:8888/actuator/health`）
> 
> **🔄 图片 URL 不可用时的处理：**
> - 如果上述示例中的图片 URL 因 CDN 限制、链接失效或其他原因无法访问，**请自行寻找合适的替代图片**。
> - 建议使用的稳定图片源：
>   - 澎湃新闻（`imagecloud.thepaper.cn`）- 新闻类图片
>   - QuickChart.io - 图表生成
>   - CSDN 博客图片（`i-blog.csdnimg.cn`）- 代码截图
>   - GitHub raw content - 开源项目图片
> - 避免使用会拒绝 Java `UrlResource` 请求的 CDN（如百度图片、网易云部分 CDN）。
> - 如需使用其他图片，请确保 URL 可被服务端直接下载（可通过 `curl -I <URL>` 测试）。
>
> **💡 查看完整中文输出的方法：**
> - 视觉识别接口返回的 JSON 中包含大量中文内容，默认情况下可能会被转义为 Unicode 编码（如 `\u56fe2`）。
> - 如需正确显示中文，请使用以下命令：
>   ```bash
>   curl -s -X POST "http://localhost:8888/ai/vision/compare" \
>     -d "imageUrl1=..." \
>     -d "imageUrl2=..." | python3 -c "import sys, json; print(json.dumps(json.load(sys.stdin), ensure_ascii=False, indent=2))"
>   ```
> - 该命令会将 JSON 中的 Unicode 编码转换为可读的中文字符。

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| 服务注册失败 | 检查 Nacos 是否启动，环境变量是否设置 |
| 端口冲突 | 检查端口是否被占用，或修改 application.yml 中的端口 |
| gRPC 调用失败 | 确认 grpc-server 的 gRPC 端口 9090 可访问 |
| Sentinel 未限流 | 检查 Nacos 中是否配置了对应的 Sentinel 规则 |
| AI 模块 401 | 检查 OPENAI_API_KEY 是否正确配置 |
| AI 视觉识别 500 | 图片 URL 不可访问（百度图片会拒绝 Java UrlResource 请求），使用稳定可访问的 URL |
| AI 接口 400 | 中文参数需 URL 编码，使用 `--get --data-urlencode` |

## 分支说明

- `springboot3`：基于 Spring Boot 3.5.0+ 的示例
- `eureka`：使用 Eureka 作为注册中心的初始版本
