# Spring AI 示例模块

本模块演示了如何在 Spring Boot 4.0 + Spring AI 2.0 中集成和使用大语言模型。

## 功能特性

- ✅ 简单聊天对话
- ✅ 流式输出（SSE）
- ✅ 结构化输出（JSON 提取）
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

## 技术栈

- **Spring Boot**: 4.1.0
- **Spring AI**: 2.0.0
- **Jackson**: 3.x
- **Java**: 17+
- **模型提供商**: 通义千问 qwen3.7-plus（兼容 OpenAI API）

## Spring AI 2.0 核心概念

本示例展示了 Spring AI 2.0 的核心特性：

1. **ChatClient API** - 统一的聊天客户端接口，通过 `ChatClient.Builder` 构建
2. **结构化输出** - 直接将 AI 响应映射到 Java 对象
3. **流式支持** - 基于 Reactor 的响应式流式输出
4. **构造函数注入** - 推荐使用构造函数注入而非字段注入
5. **空安全** - 使用 JSpecify 注解提供空安全保证

## 注意事项

⚠️ **重要提示：**

1. Spring AI 2.0 需要 Spring Boot 4.0+ 和 Java 17+
2. 首次使用前请确保已配置有效的 API Key（通义千问 API Key 可在阿里云 DashScope 控制台获取）
3. 生产环境建议使用环境变量或密钥管理服务存储 API Key
4. 流式接口返回的是 SSE 格式，浏览器可直接订阅
5. 本项目默认使用通义千问 qwen3.7-plus 模型，Base URL 为 `https://dashscope.aliyuncs.com/compatible-mode`

## 扩展阅读

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI 2.0 新特性](https://spring.io/blog/2026/05/spring-ai-2-0-ga)
- [通义千问 DashScope 文档](https://help.aliyun.com/zh/model-studio/)
- [OpenAI API 文档](https://platform.openai.com/docs/api-reference)
