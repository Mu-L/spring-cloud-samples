# 🤖 Spring AI RAG 演示

基于 **Spring AI 2.0** 的检索增强生成模块，支持 **PgVector** 和 **Redis (RediSearch)** 两种向量存储，通过 Profile 一键切换，业务代码零改动。

| Profile    | 向量库                      | 前置条件                         | 特点                     |
|------------|--------------------------|------------------------------|------------------------|
| `pgvector` | PostgreSQL + pgvector    | PostgreSQL + pgvector 扩展     | 持久化存储，支持 SQL + 向量混合查询  |
| `redis`    | Redis Stack (RediSearch) | Redis Stack（含 RediSearch 模块） | 内存级检索，HNSW/FLAT 索引，低延迟 |

## 前置条件

**PgVector 方式（默认）**
```shell
brew install postgresql
brew install pgvector
# 初始化数据库（创建用户、数据库、启用 pgvector 扩展、建表）
psql -U postgres -f cloud-ai-rag-sample/init_ai_demo.sql
```

**Redis 方式**
```shell
# 需要 RediSearch 模块支持向量搜索，可通过 Redis Stack 或 Redis OSS + 手动加载模块实现
redis-server --port 6379 --daemonize yes \
  --loadmodule ~/Downloads/redis-oss-8.8.0-arm64/lib/redis/modules/redisearch.so \
  --loadmodule ~/Downloads/redis-oss-8.8.0-arm64/lib/redis/modules/rejson.so \
  --loadmodule ~/Downloads/redis-oss-8.8.0-arm64/lib/redis/modules/redisbloom.so \
  --loadmodule ~/Downloads/redis-oss-8.8.0-arm64/lib/redis/modules/redistimeseries.so
```

## 启动与切换

```shell
# 默认使用 pgvector
java -jar cloud-ai-rag-sample/target/cloud-ai-rag-sample.jar

# 切换到 redis 向量库
java -jar cloud-ai-rag-sample/target/cloud-ai-rag-sample.jar --spring.profiles.active=redis
```

> 实现原理：两个 VectorStore starter 同时在 classpath，通过 `spring.autoconfigure.exclude` 在每个 Profile 中互斥排除对方的自动配置类，保证同一时刻只有一个 `VectorStore` Bean。

## RAG 接口

| 接口                         | 说明            |
|----------------------------|---------------|
| `POST /ai/rag/ingest`      | 摄入文档到向量数据库    |
| `GET /ai/rag/query`        | 基于知识库的 RAG 问答 |
| `DELETE /ai/rag/documents` | 删除指定来源的文档     |

```shell
# 摄入文档
curl -X POST http://localhost:8889/ai/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"content":"Spring Cloud Alibaba 是 Spring Cloud 生态中对阿里巴巴开源中间件的集成方案...","source":"spring-cloud-alibaba-docs"}'

# RAG 查询（topK 控制检索文档数量，默认 3）
curl --get --data-urlencode "question=Spring Cloud Alibaba 有哪些核心组件？" "http://localhost:8889/ai/rag/query?topK=3"

# 删除指定来源文档
curl -X DELETE "http://localhost:8889/ai/rag/documents?source=spring-cloud-alibaba-docs"
```

> 完整 RAG 流程：文档摄入 → TokenTextSplitter 自动分块 → 向量化存储（PgVector / Redis） → 相似性检索 → 上下文增强 Prompt → LLM 生成。当知识库无相关文档时自动降级为纯 LLM 回答。

> 完整的 curl 命令示例和验证流程请参考 [SKILL.md](../SKILL.md) 中的 Spring AI RAG 章节。
