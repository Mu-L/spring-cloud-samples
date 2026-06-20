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
- ✅ 支持通义千问模型（兼容 OpenAI API）

## 快速开始

### 1. 配置 API Key

在 `application.yml` 中配置您的 API Key，或者设置环境变量：

```bash
export OPENAI_API_KEY=your-api-key-here
```

### 2. 配置模型

本项目默认使用通义千问 qwen3.7-plus 模型。

**获取 API Key：**
1. 访问 [阿里云 DashScope](https://dashscope.console.aliyun.com/)
2. 注册/登录账号
3. 在「API-KEY管理」中创建 API Key
4. 设置环境变量：`export OPENAI_API_KEY=your-api-key-here`

**配置示例：**
```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        options:
          model: qwen-plus
```
如果要测试多模态，请将model设置为支持的模型，如`qwen3.7-plus`

### 3. 启动应用

```bash
mvn spring-boot:run
```

## API 接口

### 1. 简单聊天

**请求：**
```bash
curl "http://localhost:8090/ai/chat?message=你好，请介绍一下自己"
```

**响应：**
```
你好！我是人工智能助手...
```

### 2. 流式聊天（SSE）

**请求：**
```bash
curl http://localhost:8090/ai/chat/stream?message="讲一个简短的故事"
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
curl "http://localhost:8090/ai/extract?text=张三今年25岁，是一名软件工程师，邮箱是zhangsan@example.com"
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
curl -X POST "http://localhost:8090/ai/advanced/system-message?message=如何设计一个高并发的秒杀系统？"
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
curl -X POST "http://localhost:8090/ai/advanced/few-shot?message=创建一个Map，key为用户名，value为年龄"
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
curl -X POST "http://localhost:8090/ai/advanced/conversation?currentMessage=你还记得我刚才说了什么吗？"
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
curl -X POST "http://localhost:8090/ai/advanced/creative?message=写一首关于春天的短诗"
```

### 8. 多模态 - 图片分析（URL）

> ⚠️ 需要将 model 改为支持多模态的模型，如 `qwen3.7-plus`

**请求：**
```bash
curl -X POST "http://localhost:8090/ai/vision/analyze-url?imageUrl=https://example.com/photo.jpg&prompt=请描述这张图片"
```

### 9. 多模态 - 上传图片分析

**请求：**
```bash
curl -X POST -F "file=@/path/to/image.jpg" "http://localhost:8090/ai/vision/analyze-upload"
```

### 10. 多模态 - OCR 文字识别

**请求：**
```bash
curl -X POST "http://localhost:8090/ai/vision/ocr?imageUrl=https://example.com/text-image.png"
```

### 11. 多模态 - 图表分析

**请求：**
```bash
curl -X POST "http://localhost:8090/ai/vision/chart-analysis?imageUrl=https://example.com/chart.png"
```

### 12. 多模态 - 代码截图转代码

**请求：**
```bash
curl -X POST "http://localhost:8090/ai/vision/code-from-image?imageUrl=https://example.com/code-screenshot.png"
```

### 13. 多模态 - 多图片对比

**请求：**
```bash
curl -X POST "http://localhost:8090/ai/vision/compare?imageUrl1=https://example.com/img1.jpg&imageUrl2=https://example.com/img2.jpg"
```

## 项目结构

```
cloud-ai-sample/
├── src/main/java/org/hongxi/cloud/sample/ai/
│   ├── AiApplication.java                    # 启动类
│   ├── controller/
│   │   ├── AiChatController.java             # 基础聊天接口（对话、流式、结构化输出）
│   │   ├── AdvancedChatController.java       # 高级聊天接口（System Message、Few-shot、多轮对话）
│   │   └── VisionController.java             # 多模态图像处理接口
│   ├── service/
│   │   ├── AiChatService.java                # 基础聊天服务
│   │   ├── AdvancedChatService.java          # 高级聊天服务
│   │   └── VisionService.java                # 多模态图像处理服务
│   └── vo/
│       └── PersonInfo.java                   # 结构化输出 VO（record）
└── src/main/resources/
    └── application.yml                        # 配置文件
```

## 技术栈

- **Spring Boot**: 4.1.0
- **Spring AI**: 2.0.0
- **Java**: 17+
- **模型提供商**: 通义千问（兼容 OpenAI API）
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
8. **构造函数注入** - 推荐使用构造函数注入 `ChatClient.Builder`

## 注意事项

⚠️ **重要提示：**

1. Spring AI 2.0 需要 Spring Boot 4.0+ 和 Java 17+
2. 首次使用前请确保已配置有效的 API Key（通义千问 API Key 可在阿里云 DashScope 控制台获取）
3. 生产环境建议使用环境变量或密钥管理服务存储 API Key
4. 流式接口返回的是 SSE 格式，浏览器可直接订阅
5. 多模态功能（Vision）需要使用支持多模态的模型，如 `qwen3.7-plus`，可在 `application.yml` 中修改 model 配置
6. 本项目默认使用通义千问 qwen-plus 模型，Base URL 为 `https://dashscope.aliyuncs.com/compatible-mode/v1`

## 扩展阅读

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI 2.0 新特性](https://spring.io/blog/2026/05/spring-ai-2-0-ga)
- [通义千问 DashScope 文档](https://help.aliyun.com/zh/model-studio/)
- [OpenAI API 文档](https://platform.openai.com/docs/api-reference)
