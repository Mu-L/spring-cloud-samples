package org.hongxi.cloud.sample.ai.rag.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedding 批量策略配置
 * <p>
 * DashScope text-embedding-v3 限制单次请求不超过 10 条文档，
 * 默认的 TokenCountBatchingStrategy 按 token 数分批，无法适配此限制。
 * 此处提供按文档数量分批的策略，每批最多 10 条。
 * </p>
 *
 * @author javahongxi
 */
@Configuration
public class EmbeddingConfig {

    /**
     * DashScope embedding API 单次最多接受 10 条文档
     */
    private static final int DASHSCOPE_MAX_BATCH_SIZE = 10;

    @Bean
    public BatchingStrategy batchingStrategy() {
        return documents -> {
            List<List<Document>> batches = new ArrayList<>();
            for (int i = 0; i < documents.size(); i += DASHSCOPE_MAX_BATCH_SIZE) {
                batches.add(documents.subList(i, Math.min(i + DASHSCOPE_MAX_BATCH_SIZE, documents.size())));
            }
            return batches;
        };
    }
}
