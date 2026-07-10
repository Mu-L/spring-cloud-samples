# 🤖 Spring AI 演示

基于 **Spring AI 2.0**，集成阿里云百炼（DashScope）兼容 OpenAI 协议。

> ⏱️ **耗时提示**：AI 接口调用大模型 API，每次响应通常需 **5~30 秒**，完整演示所有 AI 功能约需 **5~10 分钟**。
> 建议：
> - 所有 AI curl 命令加 `--max-time 60` 防止无限等待
> - 响应内容用 `| head -c 500` 截断，避免刷屏，加速演示
> - 多轮对话演示 **2 轮**即可体现上下文记忆能力，无需执行 3 轮
> - 视觉识别 6 个接口可并行发起（用 `&&` 串联），减少等待

前置条件：配置 API Key
```shell
export OPENAI_API_KEY=your-api-key-here
```

启动 AI 模块（端口 8888），默认使用 `qwen-plus` 纯文本模型，视觉识别接口自动切换为 `qwen3.7-plus` 多模态模型。
```shell
./mvnw -pl cloud-ai-sample spring-boot:run

# 如需切换其他模型，可通过命令行参数覆盖
./mvnw -pl cloud-ai-sample spring-boot:run -Dspring-boot.run.arguments=--spring.ai.openai.chat.options.model=<模型名>
```

等待 AI 模块就绪（通过 actuator 健康检查）：
```shell
for i in $(seq 1 60); do
  resp=$(curl -s "http://localhost:8888/actuator/health" 2>/dev/null)
  if echo "$resp" | grep -q '"status":"UP"'; then
    echo "AI 模块已就绪 (耗时 ${i}s)"
    break
  fi
  sleep 1
done
```

## 基础能力

| 接口                | 说明        |
|-------------------|-----------|
| `/ai/chat`        | 简单聊天      |
| `/ai/chat/stream` | 流式输出（SSE） |
| `/ai/extract`     | 结构化输出     |

```shell
# 简单聊天
curl --max-time 60 --get --data-urlencode "message=你好" "http://localhost:8888/ai/chat" | head -c 500
# 流式输出
curl --max-time 60 --get --data-urlencode "message=讲一个故事" "http://localhost:8888/ai/chat/stream" | head -c 500
# 结构化输出
curl --max-time 60 --get --data-urlencode "message=张三今年25岁，是软件工程师" "http://localhost:8888/ai/extract"
```

## 高级对话

| 接口                            | 说明                      |
|-------------------------------|-------------------------|
| `/ai/advanced/system-message` | System Message 设定 AI 角色 |
| `/ai/advanced/few-shot`       | Few-shot Prompting 示例引导 |
| `/ai/advanced/conversation`   | 多轮对话（连续发送，AI 记住上下文）     |
| `/ai/advanced/creative`       | 带温度参数的创意性对话             |

```shell
# System Message 设定 AI 角色
curl --max-time 60 --get --data-urlencode "message=Dubbo 3.3 有哪些特性" "http://localhost:8888/ai/advanced/system-message" | head -c 500
# Few-shot 示例引导
curl --max-time 60 --get --data-urlencode "message=创建一个列表，包含 1, 2, 3" "http://localhost:8888/ai/advanced/few-shot"
# 多轮对话（演示 2 轮即可体现上下文记忆）
curl --max-time 60 --get --data-urlencode "message=我喜欢Java和Spring Boot" "http://localhost:8888/ai/advanced/conversation" | head -c 500
curl --max-time 60 --get --data-urlencode "message=那我应该用什么技术栈来做微服务" "http://localhost:8888/ai/advanced/conversation" | head -c 500
# 带温度参数的创意性对话
curl --max-time 60 --get --data-urlencode "message=帮我写一篇春天的故事，不超过300字" "http://localhost:8888/ai/advanced/creative" | head -c 500
```

## Tool Calling & MCP Server

| 接口                         | 说明                         |
|----------------------------|----------------------------|
| `/ai/tool/weather`         | 天气查询（AI 自动调用 WeatherTools） |
| `/ai/tool/time`            | 时间查询（AI 自动调用 TimeTools）    |
| `/ai/tool/smart-assistant` | 智能助手（自动选择合适的工具）            |
| `/ai/agent/chat`           | ReAct Agent（多步推理 + 工具组合）   |
| `/ai/demo`                 | 项目演示 Agent（自主调用工具验证本项目）    |

```shell
# Tool Calling
curl --max-time 60 --get --data-urlencode "message=北京今天天气怎么样？" "http://localhost:8888/ai/tool/weather" | head -c 500
# ReAct Agent
curl --max-time 60 --get --data-urlencode "message=北京天气怎么样？适合出门吗？" "http://localhost:8888/ai/agent/chat" | head -c 500
```

通过 SSE 端点 `http://localhost:8888/sse` 暴露工具，支持跨进程 Agent 通信。

## 多模态视觉识别

| 接口                           | 说明       |
|------------------------------|----------|
| `/ai/vision/analyze-url`     | URL 图片分析 |
| `/ai/vision/analyze-upload`  | 上传图片分析   |
| `/ai/vision/ocr`             | OCR 文字识别 |
| `/ai/vision/chart-analysis`  | 图表分析     |
| `/ai/vision/code-from-image` | 代码截图转代码  |
| `/ai/vision/compare`         | 多图片对比    |

**🔍 验证前预检查图片 URL 可用性：**

调用视觉接口前，必须先检查以下 6 个图片 URL 是否可访问（curl -I 检查），不可用的需从今日头条、澎湃新闻找替代图片。图片要求：公开可访问、不拒绝 Java `UrlResource` 请求（避免百度图片等限制性 CDN）。
```shell
for url in \
  "https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg" \
  "https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg" \
  "https://p3-search.byteimg.com/obj/pgc-image/94e63ee2f0f840b0813e3746d2a9590b" \
  "https://p3-search.byteimg.com/obj/labis/624fb344cca59ed91d6ada99b45f41ca" \
  "https://p3-search.byteimg.com/obj/labis/9c78113c22823e91536fb63f8f599e13" \
  "https://p3-search.byteimg.com/obj/labis/a7dd04c539c4515b6018e9a39a32be36"; do
  curl -s -o /dev/null -w "%{http_code} $url\n" -L --max-time 10 "$url"
done
```

**🔴 必须逐一演示全部 6 个视觉识别接口，不可跳过：**

```shell
# 1/6 URL 图片分析（澎湃新闻：神舟十号海报）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/analyze-url" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg" | head -c 500

# 2/6 图片上传分析（项目根目录下的架构图）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/analyze-upload" \
  -F "file=@arch.png" | head -c 500

# 3/6 OCR 文字识别（澎湃新闻：北京申奥成功）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/ocr" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg" | head -c 500

# 4/6 图表分析（今日头条：武汉市历年生产总值）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/chart-analysis" \
  -d "imageUrl=https://p3-search.byteimg.com/obj/pgc-image/94e63ee2f0f840b0813e3746d2a9590b" | head -c 500

# 5/6 代码截图转代码（今日头条：Java代码图片）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/code-from-image" \
  -d "imageUrl=https://p3-search.byteimg.com/obj/labis/624fb344cca59ed91d6ada99b45f41ca" | head -c 500

# 6/6 多图片对比分析（今日头条：鞠婧祎 vs 陈都灵）
curl --max-time 60 -X POST "http://localhost:8888/ai/vision/compare" \
  -d "imageUrl1=https://p3-search.byteimg.com/obj/labis/9c78113c22823e91536fb63f8f599e13" \
  -d "imageUrl2=https://p3-search.byteimg.com/obj/labis/a7dd04c539c4515b6018e9a39a32be36" | head -c 500
```

> **💡 中文输出**：视觉接口返回的 JSON 中文可能被 Unicode 转义，用以下命令正确显示：
> ```shell
> curl -s -X POST "http://localhost:8888/ai/vision/analyze-url" \
>   -d "imageUrl=..." | python3 -c "import sys, json; print(json.dumps(json.load(sys.stdin), ensure_ascii=False, indent=2))"
> ```

## DeepSeek 多提供商集成

同一模块内集成 DashScope + DeepSeek 两个提供商，验证 Spring AI 的多模型管理能力。需额外配置 `export DEEPSEEK_API_KEY=your-key`，未配置时跳过此节。

| 接口                         | 说明                  |
|----------------------------|---------------------|
| `/deepseek/chat`           | 简单聊天                |
| `/deepseek/chat/stream`    | 流式输出                |
| `/deepseek/system-message` | System Message 设定角色 |
| `/deepseek/creative`       | 创意性对话               |
| `/deepseek/agent/chat`     | ReAct Agent         |

```shell
# 简单聊天
curl --max-time 60 --get --data-urlencode "message=你好" "http://localhost:8888/deepseek/chat" | head -c 500
# 流式输出
curl --max-time 60 --get --data-urlencode "message=武汉简介" "http://localhost:8888/deepseek/chat/stream" | head -c 500
# System Message
curl --max-time 60 --get --data-urlencode "message=Dubbo 3.3 有哪些特性" "http://localhost:8888/deepseek/system-message" | head -c 500
# 创意性对话
curl --max-time 60 --get --data-urlencode "message=帮我写一篇春天的故事，不超过300字" "http://localhost:8888/deepseek/creative" | head -c 500
# ReAct Agent
curl --max-time 60 --get --data-urlencode "message=北京天气怎么样？适合出门吗？" "http://localhost:8888/deepseek/agent/chat" | head -c 500
```

## ChatMemory 多轮对话记忆

基于 `spring-ai-starter-model-chat-memory-repository-jdbc`，对话历史持久化到 PostgreSQL，支持会话隔离。需前置 PostgreSQL（同 RAG 模块）。

| 接口                                   | 说明       |
|--------------------------------------|----------|
| `POST /ai/memory/chat`               | 带记忆的多轮对话 |
| `DELETE /ai/memory/{conversationId}` | 清除会话记忆   |

```shell
# 第 1 轮：告诉 AI 你的名字
curl --max-time 60 -X POST http://localhost:8888/ai/memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"session-001","message":"你好，我叫小明"}' | head -c 500

# 第 2 轮：追问，AI 会记住上下文
curl --max-time 60 -X POST http://localhost:8888/ai/memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"session-001","message":"我叫什么名字？"}' | head -c 500
# AI 应回答"小明"，证明记住了上一轮对话

# 不同会话完全隔离（session-002 不知道 session-001 的内容）
curl --max-time 60 -X POST http://localhost:8888/ai/memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"session-002","message":"我叫什么名字？"}' | head -c 500
# AI 不应知道"小明"，证明会话隔离生效

# 清除会话记忆
curl --max-time 60 -X DELETE http://localhost:8888/ai/memory/session-001
```

## PromptTemplate 提示词模板

使用 Spring AI 的 `PromptTemplate` 进行 `{variable}` 占位符替换，演示三种模板场景。

| 接口                        | 说明          |
|---------------------------|-------------|
| `POST /ai/prompt/product` | 产品描述生成      |
| `POST /ai/prompt/code`    | 代码解释        |
| `POST /ai/prompt/custom`  | 自定义模板（通用入口） |

```shell
# 产品描述生成
curl --max-time 60 -X POST http://localhost:8888/ai/prompt/product \
  -H "Content-Type: application/json" \
  -d '{"product":"Spring AI 实战手册","category":"技术书籍","tone":"专业且幽默"}' | head -c 500

# 代码解释
curl --max-time 60 -X POST http://localhost:8888/ai/prompt/code \
  -H "Content-Type: application/json" \
  -d '{"code":"public record Point(int x, int y) {}","language":"Java","level":"初学者"}' | head -c 500

# 自定义模板（通用入口，支持任意变量）
curl --max-time 60 -X POST http://localhost:8888/ai/prompt/custom \
  -H "Content-Type: application/json" \
  -d '{"template":"请用{language}写一个{function}的示例代码","variables":{"language":"Python","function":"快速排序"}}' | head -c 500
```
