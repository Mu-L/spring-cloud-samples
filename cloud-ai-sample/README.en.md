## Spring AI Sample Module

This module demonstrates how to integrate and use Large Language Models in Spring Boot 4.1 + Spring AI 2.0.

### Features

- ✅ Simple chat conversations
- ✅ Streaming output (SSE)
- ✅ Structured output (JSON extraction)
- ✅ System Message role setup
- ✅ Few-shot Prompting example guidance
- ✅ Multi-turn conversations (context maintenance)
- ✅ Multimodal image processing (image analysis, OCR, chart analysis, code recognition, multi-image comparison)
- ✅ Tool Calling
- ✅ ReAct Agent (multi-step reasoning + tool calling)
- ✅ MCP Server (expose Tool services via HTTP endpoints, supporting Agent interconnection)
- ✅ Qwen LLM support (OpenAI API compatible)

### Quick Start

#### 1. Configure API Key

Configure your API Key in `application.yml`, or set the environment variable:

```bash
export OPENAI_API_KEY=your-api-key-here
```

#### 2. Configure Model

This project uses the Qwen qwen-plus model by default.

**Get API Key:**
1. Visit [Alibaba Cloud DashScope](https://dashscope.console.aliyun.com/)
2. Register / log in to your account
3. Create an API Key in "API Key Management"
4. Set environment variable: `export OPENAI_API_KEY=your-api-key-here`

**Configuration Example:**
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
Notes:
1. The 2.0 version base-url includes `/v1`
2. To test multimodal features, set the model to one that supports it, e.g. `qwen3.7-plus`

#### 3. Start the Application

```bash
mvn spring-boot:run
```

### API Endpoints

#### 1. Simple Chat

**Request:**
```bash
curl "http://localhost:8080/ai/chat?message=Hello, please introduce yourself"
```

**Response:**
```
Hello! I am an AI assistant...
```

#### 2. Streaming Chat (SSE)

**Request:**
```bash
curl "http://localhost:8080/ai/chat/stream?message=Tell me a short story"
```

**Response:**
```
data: Once upon
data: a time
data: ...
```

#### 3. Structured Output

**Request:**
```bash
curl "http://localhost:8080/ai/extract?text=John is 25 years old, a software engineer, his email is john@example.com"
```

**Response:**
```json
{
  "name": "John",
  "age": 25,
  "email": "john@example.com",
  "occupation": "Software Engineer"
}
```

#### 4. System Message Role Setup

**Request:**
```bash
curl -X POST "http://localhost:8080/ai/advanced/system-message?message=How to design a high-concurrency flash sale system?"
```

**Response:**
```json
{
  "userMessage": "How to design a high-concurrency flash sale system?",
  "aiResponse": "As a senior Java architect, let me analyze this in detail..."
}
```

#### 5. Few-shot Prompting

**Request:**
```bash
curl -X POST "http://localhost:8080/ai/advanced/few-shot?message=Create a Map with username as key and age as value"
```

**Response:**
```json
{
  "userMessage": "Create a Map with username as key and age as value",
  "aiResponse": "Map<String, Integer> userAges = new HashMap<>();"
}
```

#### 6. Multi-turn Conversation

**Request:**
```bash
curl -X POST "http://localhost:8080/ai/advanced/conversation?currentMessage=Do you remember what I just said?"
```

**Response:**
```json
{
  "currentMessage": "Do you remember what I just said?",
  "aiResponse": "You just said 'Hello'...",
  "messageCount": 1
}
```

#### 7. Creative Conversation

**Request:**
```bash
curl -X POST "http://localhost:8080/ai/advanced/creative?message=Write a short poem about spring"
```

#### 8. Multimodal - Image Analysis (URL)

> ⚠️ Requires changing the model to a multimodal-capable one, e.g. `qwen3.7-plus`

**Request:**
```bash
curl -X POST "http://localhost:8080/ai/vision/analyze-url" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/150.jpg"
```

#### 9. Multimodal - Upload Image Analysis

**Request:**
```bash
curl -X POST -F "file=@/path/to/image.jpg" "http://localhost:8080/ai/vision/analyze-upload"
```

#### 10. Multimodal - OCR Text Recognition

**Request:**
```bash
curl -X POST "http://localhost:8080/ai/vision/ocr" \
  -d "imageUrl=https://imagecloud.thepaper.cn/thepaper/image/333/857/151.jpg"
```

#### 11. Multimodal - Chart Analysis

**Request:**
```bash
curl -X POST "http://localhost:8080/ai/vision/chart-analysis" \
  -d "imageUrl=https://quickchart.io/chart?c=%7Btype%3A%27bar%27%2Cdata%3A%7Blabels%3A%5B%27Q1%27%2C%27Q2%27%2C%27Q3%27%2C%27Q4%27%5D%2Cdatasets%3A%5B%7Blabel%3A%27Revenue%27%2Cdata%3A%5B100%2C200%2C150%2C300%5D%7D%5D%7D%7D"
```

#### 12. Multimodal - Code Screenshot to Code

**Request:**
```bash
curl -X POST "http://localhost:8080/ai/vision/code-from-image" \
  -d "imageUrl=https://i-blog.csdnimg.cn/blog_migrate/486ded85cb954f0da650e7f9c306900e.png"
```

#### 13. Multimodal - Multi-image Comparison

**Request:**
```bash
curl -X POST "http://localhost:8080/ai/vision/compare" \
  -d "imageUrl1=https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0519%2F741a10acj00swie1b004ed200u00140g00zk01be.jpg&thumbnail=660x2147483647&quality=80&type=jpg" \
  -d "imageUrl2=https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0328%2Ffbc14108j00sttzpb002ld000yg00uem.jpg&thumbnail=660x2147483647&quality=80&type=jpg"
```

#### 14. Tool Calling - Weather Query

**Request:**
```bash
curl "http://localhost:8080/ai/tool/weather?question=What is the weather in Beijing today?"
```

**How it works:**
1. AI analyzes the question and identifies the need to check the weather
2. AI automatically generates a tool call: `getWeather("Beijing")`
3. Spring AI executes `WeatherTools.getWeather()`
4. AI generates a natural language response based on the returned weather data

#### 15. Tool Calling - Time Query

**Request:**
```bash
curl "http://localhost:8080/ai/tool/time?question=What time is it now?"
curl "http://localhost:8080/ai/tool/time?question=How many days until the 2027 Spring Festival?"
```

#### 16. Tool Calling - Smart Assistant (Multi-tool Auto Selection)

AI automatically selects which tools to call based on the question:

**Request:**
```bash
# AI automatically calls WeatherTools
curl "http://localhost:8080/ai/tool/ask?question=Check the weather in Shanghai for me"

# AI automatically calls TimeTools
curl "http://localhost:8080/ai/tool/ask?question=What time is it now?"

# AI automatically calls SearchTools
curl "http://localhost:8080/ai/tool/ask?question=What is Spring AI?"
```

#### 17. ReAct Agent - Smart Q&A

> ⭐ **ReAct (Reasoning + Acting)** pattern: AI combines reasoning and tool calling to solve complex problems

**Request:**
```bash
curl "http://localhost:8080/ai/agent/chat?question=What is the weather in Beijing today? Is it suitable for going out?"
```

#### 18. ReAct Agent - Complex Task Handling

The Agent breaks down complex problems, calls tools step by step to gather required information, and finally integrates a complete answer.

**Request:**
```bash
curl "http://localhost:8080/ai/agent/complex-task?task=I want to travel to Hangzhou, please check the weather in Hangzhou and introduce the city"
```

### Project Structure

```
cloud-ai-sample/
├── src/main/java/org/hongxi/cloud/sample/ai/
│   ├── AiApplication.java                    # Application entry point
│   ├── controller/
│   │   ├── AiChatController.java             # Basic chat endpoints (conversation, streaming, structured output)
│   │   ├── AdvancedChatController.java       # Advanced chat endpoints (System Message, Few-shot, multi-turn)
│   │   ├── VisionController.java             # Multimodal image processing endpoints
│   │   ├── ToolCallingController.java        # 🔥 Tool Calling endpoints
│   │   └── ReactAgentController.java         # 🔥 ReAct Agent endpoints
│   ├── service/
│   │   ├── AiChatService.java                # Basic chat service
│   │   ├── AdvancedChatService.java          # Advanced chat service
│   │   └── VisionService.java                # Multimodal image processing service
│   ├── tool/
│   │   ├── WeatherTools.java                 # 🔥 Weather tools (@Tool annotation)
│   │   ├── TimeTools.java                    # 🔥 Time tools (@Tool annotation)
│   │   ├── SearchTools.java                  # 🔥 Search tools (@Tool annotation)
│   │   ├── SystemTools.java                  # 🔥 General tools (math operations, string processing)
│   │   └── ConversionTools.java              # 🔥 Data conversion tools (URL/Base64 encoding/decoding)
│   ├── mcp/
│   │   └── McpServerConfig.java              # 🔥 MCP Server config (unified registration of all tools)
│   └── vo/
│       └── PersonInfo.java                   # Structured output VO (record)
└── src/main/resources/
    └── application.yml                        # Configuration file (OpenAI + MCP Server config)
```

### Tech Stack

- **Spring Boot**: 4.1.0
- **Spring AI**: 2.0.0
- **Java**: 17+
- **Model Provider**: Qwen (OpenAI API compatible)
- **Default Model**: qwen-plus (use qwen3.7-plus for multimodal scenarios)

### Spring AI 2.0 Core Concepts

This example demonstrates the core features of Spring AI 2.0:

1. **ChatClient API** - Unified chat client interface, built via `ChatClient.Builder`
2. **Structured Output** - Directly map AI responses to Java objects (e.g., records) via `entity()` method
3. **Streaming Support** - Reactive streaming output based on Reactor `Flux`
4. **System Message** - Set AI role and behavior via `system()` method
5. **Few-shot Prompting** - Provide examples in System Message to guide AI output format
6. **Multi-turn Conversation** - Maintain context by passing message history via `messages()` method
7. **Multimodal Support** - Pass image resources via `media()` method for image understanding and analysis
8. **Tool Calling (@Tool)** - Define tool methods with `@Tool` annotation; AI decides whether to call them
9. **ReAct Agent** - Intelligent agent pattern combining reasoning and tool calling

### Notes

⚠️ **Important Notes:**

1. Spring AI 2.0 requires Spring Boot 4.0+ and Java 17+
2. Make sure to configure a valid API Key before first use (Qwen API Key can be obtained from Alibaba Cloud DashScope console)
3. In production environments, use environment variables or secret management services to store API Keys
4. Streaming endpoints return SSE format, which browsers can directly subscribe to
5. Multimodal features (Vision) require a multimodal-capable model, e.g. `qwen3.7-plus`; modify the model setting in `application.yml`
6. This project uses Qwen qwen-plus model by default, with Base URL `https://dashscope.aliyuncs.com/compatible-mode/v1`

### MCP Server

This module includes a built-in **MCP Server** that exposes Tool services via the `/sse` endpoint after startup, callable by any MCP Client (e.g., AI assistants, IDE plugins).

**What is MCP?**
- MCP (Model Context Protocol) is a standardized communication protocol between AI Agents
- Analogy: If Tool Calling is "AI calls local methods", MCP is "AI calls remote services"

**Connection Configuration:**

Add the following configuration in your MCP Client to connect to this service:

```json
{
  "mcpServers": {
    "cloud-ai-mcp-server": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

**Tools Provided by MCP Server:**

All tool classes are located in the `tool/` package, used both for internal AI Tool Calling and exposed via MCP:

| Tool Class       | Tool Methods                                                      |
|------------------|-------------------------------------------------------------------|
| `WeatherTools`   | `getWeather`, `getWeatherForecast`                                |
| `TimeTools`      | `getCurrentTime`, `getCurrentDate`, `daysUntil`                   |
| `SearchTools`    | `search`, `getLatestNews`                                         |
| `SystemTools`    | `add`, `multiply`, `toUpperCase`, `toLowerCase`, `reverseString`  |
| `ConversionTools`| `urlEncode`, `urlDecode`, `base64Encode`, `base64Decode`, `stringLength`, `wordCount` |

### Further Reading

- [Spring AI Official Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI 2.0 New Features](https://spring.io/blog/2026/05/spring-ai-2-0-ga)
- [MCP Protocol Specification](https://modelcontextprotocol.io/)
- [Qwen DashScope Documentation](https://help.aliyun.com/zh/model-studio/)
- [OpenAI API Documentation](https://platform.openai.com/docs/api-reference)
