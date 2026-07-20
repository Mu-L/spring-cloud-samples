package org.hongxi.cloud.sample.ai.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;

import java.util.List;

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
 * ChatClient → [ToolCallingAdvisor(+300)] → [ToolCallObservationAdvisor(+400)] → ChatModel
 *                ↑ 递归驱动工具调用循环       ↑ 位于 TCA 之后，每次迭代都被观测
 * </pre>
 * <p>
 * 关键设计：本 Advisor 的 order 必须大于 ToolCallingAdvisor（+300），
 * 才能被包含在 TCA 的递归 chain.copy(this) 中，从而观测到每次迭代。
 * 如果 order 小于 TCA，则只在首次请求时触发一次，无法观测后续迭代。
 * </p>
 *
 * @author javahongxi
 * @see org.springframework.ai.chat.client.advisor.ToolCallingAdvisor
 */
public class ToolCallObservationAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ToolCallObservationAdvisor.class);

    /**
     * 设置 order 为 HIGHEST_PRECEDENCE + 400，确保在 ToolCallingAdvisor（+300）之后执行。
     * <p>
     * ToolCallingAdvisor 递归时通过 chain.copy(this) 获取"排在自己之后的 Advisor 链"，
     * 只有 order 大于 300 的 Advisor 才会被包含在递归调用中。
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
        // 根据消息历史推断当前迭代轮次
        // 每轮工具调用会增加 AssistantMessage(toolCalls) + ToolResponseMessage，共 2 条
        // 初始状态: SYSTEM + USER = 2 条 → 第 1 轮
        // 第 1 轮工具调用后: +2 条 → 第 2 轮
        List<Message> messages = request.prompt().getInstructions();
        int iteration = computeIteration(messages);

        log.info("╔═══════════════════════════════════════════════════════");
        log.info("║ [Advisor 链] 第 {} 轮调用（消息数: {}）", iteration, messages.size());
        log.info("╠═══════════════════════════════════════════════════════");

        // 打印当前消息历史（展示累积的对话上下文）
        logMessageHistory(messages);

        // 调用下游链（到 ChatModelCallAdvisor）
        long startTime = System.currentTimeMillis();
        ChatClientResponse response = chain.nextCall(request);
        long elapsed = System.currentTimeMillis() - startTime;

        // 分析响应内容
        analyzeResponse(response, iteration, elapsed);

        return response;
    }

    /**
     * 根据消息历史推断当前是第几轮迭代。
     * 初始 2 条（SYSTEM + USER），每轮工具调用增加 2 条（AssistantMessage + ToolResponseMessage）。
     */
    private int computeIteration(List<Message> messages) {
        // 统计 ToolResponseMessage 的数量即为已完成的工具调用轮次
        long toolResponseCount = messages.stream()
                .filter(m -> m instanceof ToolResponseMessage)
                .count();
        return (int) toolResponseCount + 1;
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
            } else {
                log.info("║   [{}] {}: {}", i, type, content);
            }
        }
        log.info("╚═══════════════════════════════════════════════════════");
    }

    /**
     * 分析 ChatModel 的响应，判断是否包含工具调用请求
     */
    private void analyzeResponse(ChatClientResponse response, int iteration, long elapsed) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResults() == null) {
            log.info("║ [第 {} 轮] 响应为空，耗时 {}ms", iteration, elapsed);
            return;
        }

        boolean hasToolCalls = chatResponse.getResults().stream()
                .map(Generation::getOutput)
                .anyMatch(msg -> msg.hasToolCalls());

        if (hasToolCalls) {
            log.info("║ [第 {} 轮] 模型请求工具调用（耗时 {}ms）→ ToolCallingAdvisor 将执行工具并重新进入 Advisor 链",
                    iteration, elapsed);
        } else {
            String content = chatResponse.getResults().stream()
                    .map(g -> g.getOutput().getText())
                    .reduce("", (a, b) -> a + b);
            log.info("║ [第 {} 轮] 模型返回最终响应（耗时 {}ms）→ 工具调用循环终止", iteration, elapsed);
            log.info("║ 最终回复: {}", truncate(content, 120));
        }
        log.info("╚═══════════════════════════════════════════════════════");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
