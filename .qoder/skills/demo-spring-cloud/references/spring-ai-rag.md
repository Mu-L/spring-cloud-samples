# 🤖 Spring AI RAG 演示

基于 **Spring AI 2.0** 的检索增强生成模块，支持 **PgVector** 和 **Redis (RediSearch)** 两种向量存储，通过 Profile 一键切换，业务代码零改动。

| Profile    | 向量库                      | 前置条件                         | 特点                     |
|------------|--------------------------|------------------------------|------------------------|
| `pgvector` | PostgreSQL + pgvector    | PostgreSQL + pgvector 扩展     | 持久化存储，支持 SQL + 向量混合查询  |
| `redis`    | Redis Stack (RediSearch) | Redis Stack（含 RediSearch 模块） | 内存级检索，HNSW/FLAT 索引，低延迟 |

> ⏱️ **耗时提示**：AI 接口调用大模型 API，每次响应通常需 **5~30 秒**，完整演示约需 **5~10 分钟**。
> 建议：所有 AI curl 命令加 `--max-time 60` 防止无限等待。

## 前置条件

**PgVector 方式（默认）**
```shell
brew install postgresql
brew install pgvector
# 初始化数据库（创建用户 ai_user、数据库 ai_demo、启用 pgvector 扩展、建表）
psql -U postgres -f cloud-ai-rag-sample/init_ai_demo.sql
```

**Redis 方式**

需先启动 Redis Stack（含 RediSearch 模块），默认演示 PgVector 方式，Redis 仅作备选。

## 启动与切换

```shell
export OPENAI_API_KEY=your-api-key-here

# 默认使用 pgvector
./mvnw -pl cloud-ai-rag-sample spring-boot:run

# 切换到 redis 向量库
./mvnw -pl cloud-ai-rag-sample spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=redis
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

> 实现原理：两个 VectorStore starter 同时在 classpath，通过 `spring.autoconfigure.exclude` 在每个 Profile 中互斥排除对方的自动配置类，保证同一时刻只有一个 `VectorStore` Bean。

## RAG 接口

| 接口                         | 说明            |
|----------------------------|---------------|
| `POST /ai/rag/ingest`      | 摄入文档到向量数据库    |
| `GET /ai/rag/query`        | 基于知识库的 RAG 问答 |
| `DELETE /ai/rag/documents` | 删除指定来源的文档     |

### RAG 检索增强生成全流程

演示完整的 RAG 流程：文档摄入 → TokenTextSplitter 自动分块 → 向量化存储 → 相似性检索 → 上下文增强 Prompt → LLM 生成。

```shell
# 1. 摄入第一篇文档
curl --max-time 60 -X POST http://localhost:8889/ai/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"content":"Spring AI is a comprehensive framework for Java developers to build AI-native applications. It provides unified abstractions for Chat (ChatClient), Embedding (EmbeddingModel), Prompt templates (PromptTemplate), Vector storage (VectorStore), and RAG (RetrievalAugmentor). Spring AI supports multiple LLM providers including OpenAI, Anthropic, Azure OpenAI, Ollama. Key features include Function Calling, Structured Output, observability with Micrometer and OpenTelemetry.","source":"spring-ai-docs"}'

# 2. 摄入第二篇文档
curl --max-time 60 -X POST http://localhost:8889/ai/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"content":"PgVector is a PostgreSQL extension for vector similarity search. It supports IVFFlat and HNSW index types. IVFFlat divides vectors into lists and searches a subset, good for balance between speed and accuracy. HNSW creates a hierarchical graph for fast approximate nearest neighbor search. PgVector supports cosine distance, inner product, and Euclidean distance metrics. Recommended dimensions: 1536 for OpenAI embeddings.","source":"pgvector-docs"}'

# 3. RAG 基础查询（topK=3，检索最相关的 3 个文档片段）
curl --max-time 60 --get --data-urlencode "question=What are the core features of Spring AI?" "http://localhost:8889/ai/rag/query?topK=3" | head -c 800
# AI 回答中应包含 Spring AI 的核心特性（来自参考资料）

# 4. topK 对比（topK=1，仅检索 1 个最相关文档）
curl --max-time 60 --get --data-urlencode "question=What are the core features of Spring AI?" "http://localhost:8889/ai/rag/query?topK=1" | head -c 800

# 5. 跨文档语义检索（查询同时涉及两篇文档的内容）
curl --max-time 60 --get --data-urlencode "question=What index types and distance metrics does the vector store support?" "http://localhost:8889/ai/rag/query?topK=2" | head -c 800
# AI 应精确回答 IVFFlat、HNSW 索引类型和 cosine/inner product/Euclidean 距离度量

# 6. 删除文档后 RAG 降级验证（删除所有文档后查询，AI 走纯 LLM 路径）
curl --max-time 60 -X DELETE "http://localhost:8889/ai/rag/documents?source=spring-ai-docs"
curl --max-time 60 -X DELETE "http://localhost:8889/ai/rag/documents?source=pgvector-docs"
curl --max-time 60 --get --data-urlencode "question=What is PgVector?" "http://localhost:8889/ai/rag/query?topK=3" | head -c 800
# 回答中不应出现"参考资料"字样，确认走了纯 LLM 路径
```

> **长文档自动分块验证**：摄入超过 800 token 的长文档，TokenTextSplitter 会自动拆分为多个 chunk。
> ```shell
> # 生成约 12000 字符的长文档（~500 字符重复 200 次）
> LONG_CONTENT=$(python3 -c "print('Spring AI is a comprehensive framework for Java developers. ' * 200)")
> curl --max-time 60 -X POST http://localhost:8889/ai/rag/ingest \
>   -H "Content-Type: application/json" \
>   -d "{\"content\":\"$LONG_CONTENT\",\"source\":\"spring-ai-long-doc\"}"
> # 预期返回 chunks > 1，验证 TokenTextSplitter 自动分块
> ```
