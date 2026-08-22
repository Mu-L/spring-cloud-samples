package org.hongxi.cloud.sample.ai.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 工具调用可观测 Advisor
 * <p>
 * 演示 Spring AI 2.0 的核心架构升级：工具调用循环从 ChatModel 内部的"黑盒"
 * 提升为 Advisor 链中的"一等公民"。
 * </p>
 * <p>
 * 在 Spring AI 1.x 中，工具调用循环深埋在 ChatModel 内部，开发者无法：
 * <ul>
 *   <li>观测工具调用的中间步骤</li>
 *   <li>在工具调用前后插入自定义逻辑（日志、校验、重试）</li>
 *   <li>将工具调用与其他行为组合（限流、鉴权、审计）</li>
 * </ul>
 * </p>
 * <p>
 * Spring AI 2.0 通过 {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor}
 * 将工具调用循环提升为 Advisor 链的一部分。本 Advisor 作为一个自定义拦截器，
 * 可以在工具调用循环的每次迭代中被触发，从而实现对工具调用过程的完整观测。
 * </p>
 * <p>
 * Advisor 链执行流程（按 order 升序执行）：
 * <pre>
 * ChatClient → [ToolCallingAdvisor(+300)] → [ToolCallObservationAdvisor(+400)] → ChatModelCallAdvisor
 *                ↑ do-while 驱动工具调用循环     ↑ 位于 TCA 之后，每次迭代都被观测
 * </pre>
 * <p>
 * 关键设计：本 Advisor 的 order 必须大于 ToolCallingAdvisor（+300）
 * </p>
 * <p>
 * 同时实现 {@link CallAdvisor} 和 {@link StreamAdvisor}：流式调用时，
 * DefaultChatClient 只会把 StreamAdvisor 加入流式链，只实现 CallAdvisor 会被静默跳过。
 * </p>
 *
 * @author javahongxi
 * @see org.springframework.ai.chat.client.advisor.ToolCallingAdvisor
 */
public class ToolCallObservationAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ToolCallObservationAdvisor.class);

    /**
     * 设置 order 为 HIGHEST_PRECEDENCE + 400，确保在 ToolCallingAdvisor（+300）之后执行。
     * <p>
     * ToolCallingAdvisor 循环迭代时通过 chain.copy(this) 获取"排在自己之后的 Advisor 链"，
     * 只有 order 大于 300 的 Advisor 才会在循环迭代中被执行。
     * </p>
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 400;
    }

    @Override
    public String getName() {
        return "ToolCallObservationAdvisor";
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        List<Message> messages = request.prompt().getInstructions();
        long iteration = computeIteration(messages);
        logIterationStart(iteration, messages);

        // Next Call
        long startTime = System.currentTimeMillis();
        ChatClientResponse response = chain.nextCall(request);
        long elapsed = System.currentTimeMillis() - startTime;

        // 分析响应内容（单个完整响应，直接判断是否包含工具调用）
        boolean hasToolCalls = hasToolCalls(response);
        logIterationEnd(iteration, elapsed, hasToolCalls);

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // 使用 Flux.defer 保证日志在订阅（即 ToolCallingAdvisor 每轮迭代）时才打印，
        // 且此时 request 中的消息历史已包含前几轮工具调用的累积上下文。
        return Flux.defer(() -> {
            List<Message> messages = request.prompt().getInstructions();
            long iteration = computeIteration(messages);
            logIterationStart(iteration, messages);

            long startTime = System.currentTimeMillis();
            // 流式下工具调用信息分散在多个 chunk 中，逐块扫描并累积判断
            AtomicBoolean hasToolCalls = new AtomicBoolean(false);
            return chain.nextStream(request)
                    .doOnNext(response -> {
                        if (hasToolCalls(response)) {
                            hasToolCalls.set(true);
                        }
                    })
                    .doOnComplete(() -> logIterationEnd(iteration,
                            System.currentTimeMillis() - startTime, hasToolCalls.get()));
        });
    }

    /**
     * 根据消息历史推断当前是第几轮迭代。
     * 初始 2 条（SYSTEM + USER），每轮工具调用增加 2 条（AssistantMessage + ToolResponseMessage）。
     */
    private long computeIteration(List<Message> messages) {
        // 统计 ToolResponseMessage 的数量即为已完成的工具调用轮次
        long toolResponseCount = messages.stream()
                .filter(m -> m instanceof ToolResponseMessage)
                .count();
        return toolResponseCount + 1;
    }

    /**
     * 打印当前轮次的起始日志和消息历史，展示 ToolCallingAdvisor 如何在每次迭代中累积对话上下文。
     * call 和 stream 两种模式共用。
     */
    private void logIterationStart(long iteration, List<Message> messages) {
        log.info("╔═══════════════════════════════════════════════════════");
        log.info("║ [Advisor 链] 第 {} 轮调用（消息数: {}）", iteration, messages.size());
        log.info("╠═══════════════════════════════════════════════════════");
        logMessageHistory(messages);
    }

    /**
     * 打印消息历史，展示 ToolCallingAdvisor 如何在每次迭代中累积对话上下文
     */
    private void logMessageHistory(List<Message> messages) {
        log.info("║ 当前消息历史（共 {} 条）:", messages.size());
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            String type = msg.getMessageType().name();
            String content = truncate(msg.getText(), 80);

            if (msg instanceof ToolResponseMessage toolMsg) {
                // 工具响应消息：展示工具名称和返回结果
                log.info("║   [{}] {} (responses={})", i, type, toolMsg.getResponses().size());
                for (ToolResponseMessage.ToolResponse resp : toolMsg.getResponses()) {
                    log.info("║       → tool: {}, result: {}", resp.name(), truncate(resp.responseData(), 60));
                }
            } else if (msg instanceof AssistantMessage assistantMsg && assistantMsg.hasToolCalls()) {
                // AI 工具调用请求：展示要调用的工具
                log.info("║   [{}] {} (toolCalls={})", i, type, assistantMsg.getToolCalls().size());
                assistantMsg.getToolCalls().forEach(tc ->
                        log.info("║       → call: {}({})", tc.name(), truncate(tc.arguments(), 60)));
            } else if (msg instanceof SystemMessage) {
                log.info("║   [{}] {}: {}", i, type, truncate(content, 25));
            } else {
                log.info("║   [{}] {}: {}", i, type, content);
            }
        }
        log.info("╚═══════════════════════════════════════════════════════");
    }

    /**
     * 判断响应中是否包含工具调用请求（call 和 stream 两种模式共用）
     */
    private boolean hasToolCalls(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return false;
        }
        return chatResponse.getResults().stream()
                .map(Generation::getOutput)
                .anyMatch(AssistantMessage::hasToolCalls);
    }

    /**
     * 打印当前轮次的结束日志，判断模型是否决定调用工具。
     * call 和 stream 两种模式共用。
     */
    private void logIterationEnd(long iteration, long elapsed, boolean hasToolCalls) {
        if (hasToolCalls) {
            log.info("║ [第 {} 轮] 大模型响应（耗时 {}ms）→ hasToolCalls=true，模型决定调用工具", iteration, elapsed);
        } else {
            log.info("║ [第 {} 轮] 模型返回最终响应（耗时 {}ms）→ 工具调用循环终止", iteration, elapsed);
        }
        log.info("╚═══════════════════════════════════════════════════════");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
