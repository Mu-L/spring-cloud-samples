# 🤖 Spring AI 演示

基于 **Spring AI 2.0**，集成阿里云百炼（DashScope）兼容 OpenAI 协议。

前置条件：配置 API Key
```shell
export OPENAI_API_KEY=your-api-key-here
```

启动 AI 模块（端口 8888），默认使用 `qwen-plus` 纯文本模型，视觉识别接口自动切换为 `qwen3.7-plus` 多模态模型。

## 基础能力

| 接口                | 说明        | 示例                                                                                        |
|-------------------|-----------|-------------------------------------------------------------------------------------------|
| `/ai/chat`        | 简单聊天      | `curl --get --data-urlencode "message=你好" "http://localhost:8888/ai/chat"`                |
| `/ai/chat/stream` | 流式输出（SSE） | `curl --get --data-urlencode "message=讲一个故事" "http://localhost:8888/ai/chat/stream"`      |
| `/ai/extract`     | 结构化输出     | `curl --get --data-urlencode "message=张三今年25岁，是软件工程师" "http://localhost:8888/ai/extract"` |

## 高级对话

| 接口                            | 说明                      |
|-------------------------------|-------------------------|
| `/ai/advanced/system-message` | System Message 设定 AI 角色 |
| `/ai/advanced/few-shot`       | Few-shot Prompting 示例引导 |
| `/ai/advanced/conversation`   | 多轮对话（连续发送，AI 记住上下文）     |
| `/ai/advanced/creative`       | 带温度参数的创意性对话             |

## Tool Calling & MCP Server

| 接口                         | 说明                         |
|----------------------------|----------------------------|
| `/ai/tool/weather`         | 天气查询（AI 自动调用 WeatherTools） |
| `/ai/tool/time`            | 时间查询（AI 自动调用 TimeTools）    |
| `/ai/tool/smart-assistant` | 智能助手（自动选择合适的工具）            |
| `/ai/agent/chat`           | ReAct Agent（多步推理 + 工具组合）   |
| `/ai/demo`                 | 项目演示 Agent（自主调用工具验证本项目）    |

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

## DeepSeek 多提供商集成

同一模块内集成 DashScope + DeepSeek 两个提供商，验证 Spring AI 的多模型管理能力。需额外配置 `export DEEPSEEK_API_KEY=your-key`。

| 接口                         | 说明                  |
|----------------------------|---------------------|
| `/deepseek/chat`           | 简单聊天                |
| `/deepseek/chat/stream`    | 流式输出                |
| `/deepseek/system-message` | System Message 设定角色 |
| `/deepseek/creative`       | 创意性对话               |
| `/deepseek/agent/chat`     | ReAct Agent         |

> 完整的 curl 命令示例和验证流程请参考 [SKILL.md](../SKILL.md) 中的 Spring AI 章节。

## ChatMemory 多轮对话记忆

基于 `spring-ai-starter-model-chat-memory-repository-jdbc`，对话历史持久化到 PostgreSQL，支持会话隔离。需前置 PostgreSQL（同 RAG 模块）。

| 接口                                   | 说明       |
|--------------------------------------|----------|
| `POST /ai/memory/chat`               | 带记忆的多轮对话 |
| `DELETE /ai/memory/{conversationId}` | 清除会话记忆   |

```shell
# 第 1 轮：告诉 AI 你的名字
curl -X POST http://localhost:8888/ai/memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"session-001","message":"你好，我叫小明"}'

# 第 2 轮：追问，AI 会记住上下文
curl -X POST http://localhost:8888/ai/memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"session-001","message":"我叫什么名字？"}'

# 不同会话完全隔离
curl -X POST http://localhost:8888/ai/memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"session-002","message":"我叫什么名字？"}'

# 清除会话记忆
curl -X DELETE http://localhost:8888/ai/memory/session-001
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
curl -X POST http://localhost:8888/ai/prompt/product \
  -H "Content-Type: application/json" \
  -d '{"product":"Spring AI 实战手册","category":"技术书籍","tone":"专业且幽默"}'

# 代码解释
curl -X POST http://localhost:8888/ai/prompt/code \
  -H "Content-Type: application/json" \
  -d '{"code":"public record Point(int x, int y) {}","language":"Java","level":"初学者"}'

# 自定义模板
curl -X POST http://localhost:8888/ai/prompt/custom \
  -H "Content-Type: application/json" \
  -d '{"template":"请用{language}写一个{function}的示例代码","variables":{"language":"Python","function":"快速排序"}}'
```
