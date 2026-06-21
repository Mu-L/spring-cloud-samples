# Spring AI 示例模块

本模块演示了如何在 Spring Boot 4.1 + Spring AI 2.0 中集成和使用大语言模型。

## 功能特性

- ✅ 简单聊天对话
- ✅ 流式输出（SSE）
- ✅ 结构化输出（JSON 提取）
- ✅ System Message 角色设定
- ✅ Few-shot Prompting 示例引导
- ✅ 多轮对话（上下文维护）
- ✅ 多模态图像处理（图片分析、OCR、图表分析、代码识别、多图对比）
- ✅ Tool Calling 工具调用
- ✅ ReAct Agent 智能体（多步推理 + 工具调用）
- ✅ MCP Server（通过 HTTP 端点暴露 Tool 服务，支持 Agent 互联）
- ✅ 支持千问大模型（兼容 OpenAI API）

## 快速开始

### 1. 配置 API Key

在 `application.yml` 中配置您的 API Key，或者设置环境变量：

```bash
export OPENAI_API_KEY=your-api-key-here
```

### 2. 配置模型

本项目默认使用千问 qwen-plus 模型。

**获取 API Key：**
1. 访问 [阿里云 DashScope](https://dashscope.console.aliyun.com/)
2. 注册/登录账号
3. 在「API-KEY管理」中创建 API Key
4. 设置环境变量：`export OPENAI_API_KEY=your-api-key-here`

**配置示例：**
```yaml
spring:
  application:
    name: ai-sample
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        options:
          model: qwen-plus
          temperature: 0.7
```
注意：
1. 2.0 版本的 base-url 带了 `/v1`
2. 如果要测试多模态，请将model设置为支持的模型，如`qwen3.7-plus`

### 3. 启动应用

```bash
mvn spring-boot:run
```

## API 接口

### 1. 简单聊天

**请求：**
```bash
curl "http://localhost:8080/ai/chat?message=你好，请介绍一下自己"
```

**响应：**
```
你好！我是人工智能助手...
```

### 2. 流式聊天（SSE）

**请求：**
```bash
curl "http://localhost:8080/ai/chat/stream?message=讲一个简短的故事"
```

**响应：**
```
data: 从前
data: 有座山
data: ...
```

### 3. 结构化输出

**请求：**
```bash
curl "http://localhost:8080/ai/extract?text=张三今年25岁，是一名软件工程师，邮箱是zhangsan@example.com"
```

**响应：**
```json
{
  "name": "张三",
  "age": 25,
  "email": "zhangsan@example.com",
  "occupation": "软件工程师"
}
```

### 4. System Message 角色设定

**请求：**
```bash
curl -X POST "http://localhost:8080/ai/advanced/system-message?message=如何设计一个高并发的秒杀系统？"
```

**响应：**
```json
{
  "userMessage": "如何设计一个高并发的秒杀系统？",
  "aiResponse": "作为资深Java架构师，我来为你详细分析..."
}
```

### 5. Few-shot Prompting

**请求：**
```bash
curl -X POST "http://localhost:8080/ai/advanced/few-shot?message=创建一个Map，key为用户名，value为年龄"
```

**响应：**
```json
{
  "userMessage": "创建一个Map，key为用户名，value为年龄",
  "aiResponse": "Map<String, Integer> userAges = new HashMap<>();"
}
```

### 6. 多轮对话

**请求：**
```bash
curl -X POST "http://localhost:8080/ai/advanced/conversation?currentMessage=你还记得我刚才说了什么吗？"
```

**响应：**
```json
{
  "currentMessage": "你还记得我刚才说了什么吗？",
  "aiResponse": "你刚才说了'你好'...",
  "messageCount": 1
}
```

### 7. 创意性对话

**请求：**
```bash
curl -X POST "http://localhost:8080/ai/advanced/creative?message=写一首关于春天的短诗"
```

### 8. 多模态 - 图片分析（URL）

> ⚠️ 需要将 model 改为支持多模态的模型，如 `qwen3.7-plus`

**请求：**
```bash
curl -X POST "http://localhost:8080/ai/vision/analyze-url" \
  -d "imageUrl=https://img1.baidu.com/it/u=3224850734,2174446166&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=837"
```

### 9. 多模态 - 上传图片分析

**请求：**
```bash
curl -X POST -F "file=@/path/to/image.jpg" "http://localhost:8080/ai/vision/analyze-upload"
```

### 10. 多模态 - OCR 文字识别

**请求：**
```bash
curl -X POST "http://localhost:8080/ai/vision/ocr" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg"
```

### 11. 多模态 - 图表分析

**请求：**
```bash
curl -X POST "http://localhost:8080/ai/vision/chart-analysis" \
  -d "imageUrl=https://img0.baidu.com/it/u=3716881902,3785738263&fm=253&app=138&f=JPEG?w=684&h=912"
```

### 12. 多模态 - 代码截图转代码

**请求：**
```bash
curl -X POST "http://localhost:8080/ai/vision/code-from-image" \
  -d "imageUrl=https://img0.baidu.com/it/u=1426566285,94536163&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=667"
```

### 13. 多模态 - 多图片对比

**请求：**
```bash
curl -X POST "http://localhost:8080/ai/vision/compare" \
  -d "imageUrl1=https://img2.baidu.com/it/u=2499517816,3465890141&fm=253&fmt=auto&app=120&f=JPEG?w=800&h=1200" \
  -d "imageUrl2=https://img2.baidu.com/it/u=748716099,4246587362&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=753"
```

### 14. Tool Calling - 天气查询

**请求：**
```bash
curl "http://localhost:8080/ai/tool/weather?question=北京今天的天气怎么样？"
```

**工作原理：**
1. AI 分析问题，识别出需要查询天气
2. AI 自动生成工具调用：`getWeather("北京")`
3. Spring AI 执行 `WeatherTools.getWeather()` 方法
4. AI 基于返回的天气数据生成自然语言回答

### 15. Tool Calling - 时间查询

**请求：**
```bash
curl "http://localhost:8080/ai/tool/time?question=现在几点了？"
curl "http://localhost:8080/ai/tool/time?question=距离2027年春节还有多少天？"
```

### 16. Tool Calling - 智能助手（多工具自动选择）

AI 会根据问题自动选择调用哪些工具：

**请求：**
```bash
# AI 自动调用 WeatherTools
curl "http://localhost:8080/ai/tool/ask?question=帮我查一下上海的天气"

# AI 自动调用 TimeTools
curl "http://localhost:8080/ai/tool/ask?question=现在几点了？"

# AI 自动调用 SearchTools
curl "http://localhost:8080/ai/tool/ask?question=什么是Spring AI？"
```

### 17. ReAct Agent - 智能问答

> ⭐ **ReAct (Reasoning + Acting)** 模式：AI 结合推理和工具调用解决复杂问题

**请求：**
```bash
curl "http://localhost:8080/ai/agent/chat?question=北京今天的天气怎么样？适合出门吗？"
```

### 18. ReAct Agent - 复杂任务处理

Agent 会将复杂问题拆解，逐步调用工具获取所需信息，最终整合出完整答案。

**请求：**
```bash
curl "http://localhost:8080/ai/agent/complex-task?task=我想去杭州旅游，帮我查一下杭州的天气，以及介绍一下杭州"
```

## 项目结构

```
cloud-ai-sample/
├── src/main/java/org/hongxi/cloud/sample/ai/
│   ├── AiApplication.java                    # 启动类
│   ├── controller/
│   │   ├── AiChatController.java             # 基础聊天接口（对话、流式、结构化输出）
│   │   ├── AdvancedChatController.java       # 高级聊天接口（System Message、Few-shot、多轮对话）
│   │   ├── VisionController.java             # 多模态图像处理接口
│   │   ├── ToolCallingController.java        # 🔥 Tool Calling 工具调用接口
│   │   └── ReactAgentController.java         # 🔥 ReAct Agent 智能体接口
│   ├── service/
│   │   ├── AiChatService.java                # 基础聊天服务
│   │   ├── AdvancedChatService.java          # 高级聊天服务
│   │   └── VisionService.java                # 多模态图像处理服务
│   ├── tool/
│   │   ├── WeatherTools.java                 # 🔥 天气工具（@Tool 注解）
│   │   ├── TimeTools.java                    # 🔥 时间工具（@Tool 注解）
│   │   ├── SearchTools.java                  # 🔥 搜索工具（@Tool 注解）
│   │   ├── SystemTools.java                  # 🔥 通用工具（数学运算、字符串处理）
│   │   └── ConversionTools.java              # 🔥 数据转换工具（URL/Base64 编解码）
│   ├── mcp/
│   │   └── McpServerConfig.java              # 🔥 MCP Server 配置（统一注册所有工具）
│   └── vo/
│       └── PersonInfo.java                   # 结构化输出 VO（record）
└── src/main/resources/
    └── application.yml                        # 配置文件（含 OpenAI + MCP Server 配置）
```

## 技术栈

- **Spring Boot**: 4.1.0
- **Spring AI**: 2.0.0
- **Java**: 17+
- **模型提供商**: 千问（兼容 OpenAI API）
- **默认模型**: qwen-plus（多模态场景请使用 qwen3.7-plus）

## Spring AI 2.0 核心概念

本示例展示了 Spring AI 2.0 的核心特性：

1. **ChatClient API** - 统一的聊天客户端接口，通过 `ChatClient.Builder` 构建
2. **结构化输出** - 通过 `entity()` 方法直接将 AI 响应映射到 Java 对象（如 record）
3. **流式支持** - 基于 Reactor `Flux` 的响应式流式输出
4. **System Message** - 通过 `system()` 方法设定 AI 角色和行为
5. **Few-shot Prompting** - 在 System Message 中提供示例，引导 AI 按照期望格式输出
6. **多轮对话** - 通过 `messages()` 方法传入历史消息维护上下文
7. **多模态支持** - 通过 `media()` 方法传入图片资源，实现图像理解和分析
8. **Tool Calling（@Tool）** - 通过 `@Tool` 注解定义工具方法，AI 自动决定是否调用
9. **ReAct Agent** - 结合推理和工具调用的智能体模式

## 注意事项

⚠️ **重要提示：**

1. Spring AI 2.0 需要 Spring Boot 4.0+ 和 Java 17+
2. 首次使用前请确保已配置有效的 API Key（千问 API Key 可在阿里云 DashScope 控制台获取）
3. 生产环境建议使用环境变量或密钥管理服务存储 API Key
4. 流式接口返回的是 SSE 格式，浏览器可直接订阅
5. 多模态功能（Vision）需要使用支持多模态的模型，如 `qwen3.7-plus`，可在 `application.yml` 中修改 model 配置
6. 本项目默认使用千问 qwen-plus 模型，Base URL 为 `https://dashscope.aliyuncs.com/compatible-mode/v1`

## MCP Server

本模块内置了 **MCP Server**，启动后通过 `/sse` 端点对外暴露 Tool 服务，可被任何 MCP Client（如 AI 助手、IDE 插件）调用。

**MCP 是什么？**
- MCP（Model Context Protocol）是 AI Agent 之间的标准化通信协议
- 类比：如果说 Tool Calling 是「AI 调本地方法」，MCP 就是「AI 调远程服务」

**连接配置：**

在 MCP Client 中添加以下配置即可连接到本服务：

```json
{
  "mcpServers": {
    "cloud-ai-mcp-server": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

**MCP Server 提供的工具：**

所有工具类统一放在 `tool/` 包下，既用于内部 AI Tool Calling，也通过 MCP 对外暴露：

| 工具类 | 工具方法 |
|--------|----------|
| `WeatherTools` | `getWeather`、`getWeatherForecast` |
| `TimeTools` | `getCurrentTime`、`getCurrentDate`、`daysUntil` |
| `SearchTools` | `search`、`getLatestNews` |
| `SystemTools` | `add`、`multiply`、`toUpperCase`、`toLowerCase`、`reverseString` |
| `ConversionTools` | `urlEncode`、`urlDecode`、`base64Encode`、`base64Decode`、`stringLength`、`wordCount` |

## 扩展阅读

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI 2.0 新特性](https://spring.io/blog/2026/05/spring-ai-2-0-ga)
- [MCP 协议规范](https://modelcontextprotocol.io/)
- [千问 DashScope 文档](https://help.aliyun.com/zh/model-studio/)
- [OpenAI API 文档](https://platform.openai.com/docs/api-reference)
