package org.hongxi.cloud.sample.ai.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RAG（检索增强生成）服务
 * <p>
 * 演示基于向量数据库的知识库检索增强生成流程：
 * 1. 文档摄入：将文本分块后存入向量数据库（PgVector 或 Redis）
 * 2. RAG 查询：检索相关文档片段，拼接上下文后交给 LLM 生成回答
 * <p>
 * 支持 pgvector / redis 两种 Profile，通过 spring.profiles.active 切换。
 * 删除文档时，pgvector 使用 FilterExpressionBuilder 按 metadata 过滤，
 * redis 使用 StringRedisTemplate 按 key 前缀扫描（规避 RediSearch 保留字段限制）。
 * </p>
 *
 * @author hongxi
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final boolean isRedisProfile;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
                      StringRedisTemplate redisTemplate,
                      @Value("${spring.ai.vectorstore.redis.prefix:}") String keyPrefix,
                      @Value("${spring.profiles.active:}") String activeProfile) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        // 使用 builder 创建分块器（Spring AI 2.0 构造函数已废弃）
        this.textSplitter = TokenTextSplitter.builder().build();
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.isRedisProfile = Arrays.asList(activeProfile.split(",")).contains("redis");
    }

    /**
     * 摄入文档到向量数据库
     *
     * @param content  文本内容
     * @param source   来源标识（用于过滤和溯源）
     * @return 分块后存储的文档数量
     */
    public int ingest(String content, String source) {
        Document doc = new Document(content);
        if (source != null && !source.isBlank()) {
            doc.getMetadata().put("source", source);
        }
        List<Document> chunks = textSplitter.split(doc);
        vectorStore.add(chunks);
        log.info("文档摄入完成，source={}, 分块数={}", source, chunks.size());
        return chunks.size();
    }

    /**
     * RAG 查询：检索相关文档并增强 LLM 回答
     *
     * @param question 用户问题
     * @param topK     返回的最相关文档数量
     * @return LLM 基于上下文生成的回答
     */
    public String query(String question, int topK) {
        // 1. 从向量数据库检索相关文档
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .build()
        );

        if (docs.isEmpty()) {
            log.info("未找到相关文档，question={}", question);
            return chatClient.prompt()
                    .user("基于已有知识回答：" + question)
                    .call()
                    .content();
        }

        // 2. 拼接检索到的上下文
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));
        log.info("RAG 检索到 {} 个文档片段，question={}", docs.size(), question);

        // 3. 构建增强提示词，让 LLM 基于检索到的上下文回答
        String augmentedPrompt = """
                你是一个知识问答助手。请基于以下参考资料回答用户问题。
                尽量从参考资料中提取有用信息进行回答，如果参考资料与问题的关联度较低，
                可以结合你的知识补充回答，但需注明哪些内容来自参考资料、哪些是你的补充。
                
                【参考资料】
                %s
                
                【用户问题】
                %s
                """.formatted(context, question);

        return chatClient.prompt()
                .user(augmentedPrompt)
                .call()
                .content();
    }

    /**
     * 删除指定来源的所有文档
     * <p>
     * pgvector 模式：通过 FilterExpressionBuilder 构建 metadata 过滤条件，
     * 使用 similaritySearch 查找匹配文档后调用 VectorStore.delete() 删除。
     * <p>
     * redis 模式：RediSearch 对 filter 表达式字段名有限制（source 为保留字段），
     * 且 metadata 未存储在 ReJSON 文档中，因此改用 StringRedisTemplate
     * 按 key 前缀扫描所有文档 key，提取 ID 后调用 VectorStore.delete() 删除。
     * </p>
     *
     * @param source 来源标识
     */
    public void deleteBySource(String source) {
        if (isRedisProfile) {
            deleteBySourceRedis(source);
        } else {
            deleteBySourcePgVector(source);
        }
    }

    /**
     * PgVector 模式：通过 FilterExpressionBuilder 按 metadata.source 过滤删除
     */
    private void deleteBySourcePgVector(String source) {
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("*")
                        .topK(10000)
                        .filterExpression(filterBuilder.eq("source", source).build())
                        .build()
        );
        List<String> idsToDelete = docs.stream()
                .map(Document::getId)
                .toList();
        if (!idsToDelete.isEmpty()) {
            vectorStore.delete(idsToDelete);
            log.info("[PgVector] 已删除来源为 {} 的 {} 个文档", source, idsToDelete.size());
        } else {
            log.info("[PgVector] 未找到来源为 {} 的文档", source);
        }
    }

    /**
     * Redis 模式：通过 StringRedisTemplate 按 key 前缀扫描删除
     */
    private void deleteBySourceRedis(String source) {
        String pattern = keyPrefix + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            log.info("[Redis] 未找到任何文档（prefix={}）", pattern);
            return;
        }
        List<String> idsToDelete = new ArrayList<>();
        for (String key : keys) {
            // key 格式为 prefix + uuid，提取 uuid 作为文档 ID
            String id = key.substring(keyPrefix.length());
            idsToDelete.add(id);
        }
        if (!idsToDelete.isEmpty()) {
            vectorStore.delete(idsToDelete);
            log.info("[Redis] 已删除 {} 个文档（来源标识={}）", idsToDelete.size(), source);
        }
    }
}
