# 🤖 Spring AI RAG 演示

> 🔴 **共 9 个步骤，必须逐一执行，不可跳过。每步执行后确认返回结果是否符合预期。**

基于 **Spring AI 2.0** 的检索增强生成模块，使用 **PgVector** 作为向量存储。

> ⏱️ **耗时提示**：AI 接口调用大模型 API，每次响应通常需 **5~30 秒**，完整演示约需 **5~10 分钟**。
> 建议：所有 AI curl 命令加 `--max-time 60` 防止无限等待。

## 前置条件

**PgVector 方式（默认）**
```shell
brew install postgresql@18
brew install pgvector
brew services start postgresql@18
createdb $USER
psql -f init_ai_demo.sql
```

## 启动

```shell
export OPENAI_API_KEY=your-api-key-here

./mvnw -pl cloud-ai-rag-sample spring-boot:run
```

等待 RAG 模块就绪：
```shell
for i in $(seq 1 60); do
  resp=$(curl -s "http://localhost:8889/actuator/health" 2>/dev/null)
  if echo "$resp" | grep -q '"status":"UP"'; then
    echo "RAG 模块已就绪 (耗时 ${i}s)"
    break
  fi
  sleep 1
done
```

> 实现原理：使用 PgVector 作为向量存储，通过 `spring-ai-starter-vector-store-pgvector` 自动配置。

## 接口一览

### RAG 知识库接口

| 接口                           | 说明                       |
|------------------------------|--------------------------|
| `POST /ai/rag/ingest`        | 摄入文本内容到向量数据库             |
| `POST /ai/rag/ingest-file`   | 上传文件（md/pdf/docx 等）并摄入向量数据库 |
| `GET /ai/rag/query`          | 基于知识库的 RAG 问答（手动检索增强）      |
| `GET /ai/rag/query-advisor`  | 基于 QuestionAnswerAdvisor 的 RAG 问答 |
| `DELETE /ai/rag/documents`   | 删除指定来源的文档               |

### 长期记忆接口

| 接口                                     | 说明                         |
|----------------------------------------|----------------------------|
| `POST /ai/long-term-memory/chat`       | 带短期+长期记忆的多轮对话               |
| `DELETE /ai/long-term-memory/{conversationId}` | 清除指定会话的短期记忆（长期向量记忆保留）      |

> 实现原理：短期记忆通过 JDBC 持久化的 `MessageWindowChatMemory`（滑动窗口 20 条）保持当前会话连贯；
> 长期记忆通过 `VectorStoreChatMemoryAdvisor` 在 `before()` 阶段按语义相似度检索历史对话注入 system prompt，
> 在 `after()` 阶段将对话写入向量库。RAG 文档添加 `type: "knowledge"` 元数据，与记忆文档（含 `conversationId` 元数据）隔离，
> 查询时通过 `filterExpression` 确保只检索知识库文档。

---

## Step 1：摄入第一篇文档

```shell
curl --max-time 60 -X POST http://localhost:8889/ai/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"content":"Spring AI is a comprehensive framework for Java developers to build AI-native applications. It provides unified abstractions for Chat (ChatClient), Embedding (EmbeddingModel), Prompt templates (PromptTemplate), Vector storage (VectorStore), and RAG (RetrievalAugmentor). Spring AI supports multiple LLM providers including OpenAI, Anthropic, Azure OpenAI, Ollama. Key features include Function Calling, Structured Output, observability with Micrometer and OpenTelemetry.","source":"spring-ai-docs"}'
```

**预期结果**：返回 JSON，`chunks` 字段 > 0，表示文档已成功摄入。

---

## Step 2：摄入第二篇文档

```shell
curl --max-time 60 -X POST http://localhost:8889/ai/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"content":"PgVector is a PostgreSQL extension for vector similarity search. It supports IVFFlat and HNSW index types. IVFFlat divides vectors into lists and searches a subset, good for balance between speed and accuracy. HNSW creates a hierarchical graph for fast approximate nearest neighbor search. PgVector supports cosine distance, inner product, and Euclidean distance metrics. Recommended dimensions: 1536 for OpenAI embeddings.","source":"pgvector-docs"}'
```

**预期结果**：返回 JSON，`chunks` 字段 > 0。

---

## Step 3：RAG 基础查询（topK=3）

```shell
curl --max-time 60 --get --data-urlencode "question=What are the core features of Spring AI?" "http://localhost:8889/ai/rag/query?topK=3" | head -c 800
```

**预期结果**：AI 回答中应包含 Spring AI 的核心特性（来自参考资料），而非纯 LLM 生成内容。

---

## Step 4：topK 对比（topK=1）

```shell
curl --max-time 60 --get --data-urlencode "question=What are the core features of Spring AI?" "http://localhost:8889/ai/rag/query?topK=1" | head -c 800
```

**预期结果**：仅检索 1 个最相关文档，回答内容可能与 topK=3 有所不同。

---

## Step 5：不同主题文档检索验证（topK=2）

> 与 Step 3 基础查询原理相同（都是语义搜索），此处换一个与第二篇文档（pgvector-docs）相关的问题，
> 验证系统能根据问题语义精准命中不同主题的文档，而非固定返回同一篇。

```shell
curl --max-time 60 --get --data-urlencode "question=What index types and distance metrics does the vector store support?" "http://localhost:8889/ai/rag/query?topK=2" | head -c 800
```

**预期结果**：AI 应精确回答 IVFFlat、HNSW 索引类型和 cosine/inner product/Euclidean 距离度量（来自第二篇文档），而非返回第一篇 Spring AI 的内容。

---

## Step 6：删除文档后 RAG 降级验证

```shell
# 删除所有文档
curl --max-time 60 -X DELETE "http://localhost:8889/ai/rag/documents?source=spring-ai-docs"
curl --max-time 60 -X DELETE "http://localhost:8889/ai/rag/documents?source=pgvector-docs"
# 删除后查询，AI 走纯 LLM 路径
curl --max-time 60 --get --data-urlencode "question=What is PgVector?" "http://localhost:8889/ai/rag/query?topK=3" | head -c 800
```

**预期结果**：回答中**不应**出现"参考资料"字样，确认走了纯 LLM 路径（无知识库可检索）。

---

---

## Step 7：文件上传摄入（ingest-file）

> 演示通过上传本地文件（Markdown、PDF、DOCX 等）摄入知识库，使用 Tika 解析文件内容。

```shell
curl --max-time 60 -X POST "http://localhost:8889/ai/rag/ingest-file?source=ai-three-frameworks" \
  -F "file=@ai-three-frameworks.md"
```

**预期结果**：返回 JSON，`chunks` 字段 > 0（文件被解析并分块存储）。自定义 `BatchingStrategy` 每批最多 10 条文档发给 embedding API，适配 DashScope 的批量限制。

---

## Step 8：QuestionAnswerAdvisor 查询

> 使用 `QuestionAnswerAdvisor` 简化版查询，Advisor 自动完成文档检索并注入上下文到 prompt。

```shell
curl --max-time 60 --get --data-urlencode "question=Spring AI的Advisor机制是什么" "http://localhost:8889/ai/rag/query-advisor"
```

**预期结果**：AI 基于检索到的知识库文档回答，与 Step 3 的 `query` 接口效果类似，但底层由 Advisor 自动完成检索增强。

---

## Step 9：长期记忆对话

> 演示短期滑动窗口 + 长期向量检索的组合记忆能力。

**9.1 第一轮对话（告诉 AI 个人信息）：**
```shell
curl --max-time 60 -X POST http://localhost:8889/ai/long-term-memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"demo-001","message":"我叫小明，我喜欢Java编程"}'
```
**预期结果**：AI 回复中体现记住了你的名字和爱好。

**9.2 第二轮对话（验证记忆检索）：**
```shell
curl --max-time 60 -X POST http://localhost:8889/ai/long-term-memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"demo-001","message":"我叫什么名字？我喜欢什么？"}'
```
**预期结果**：AI 能准确回忆出你叫小明、喜欢 Java 编程。

**9.3 验证会话隔离（不同 conversationId）：**
```shell
curl --max-time 60 -X POST http://localhost:8889/ai/long-term-memory/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"demo-002","message":"我叫什么名字？"}'
```
**预期结果**：AI 无法回忆 demo-001 的信息，证明会话隔离正常。

---

> **长文档自动分块验证**（可选）：摄入超过 800 token 的长文档，TokenTextSplitter 会自动拆分为多个 chunk。
> ```shell
> LONG_CONTENT=$(python3 -c "print('Spring AI is a comprehensive framework for Java developers. ' * 200)")
> curl --max-time 60 -X POST http://localhost:8889/ai/rag/ingest \
>   -H "Content-Type: application/json" \
>   -d "{\"content\":\"$LONG_CONTENT\",\"source\":\"spring-ai-long-doc\"}"
> # 预期返回 chunks > 1，验证 TokenTextSplitter 自动分块
> ```
