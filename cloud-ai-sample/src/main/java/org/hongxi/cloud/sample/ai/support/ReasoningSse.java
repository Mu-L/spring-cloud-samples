package org.hongxi.cloud.sample.ai.support;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 思考内容（reasoning）SSE 流式输出支持
 * <p>
 * 将 ChatClient 的 {@code Flux<ChatResponse>} 统一转换为带事件类型的 SSE 流：
 * <ul>
 *   <li>{@code reasoning} — 模型思考内容增量（思考模型如 qwq/qwen3 会返回）</li>
 *   <li>{@code token} — 回答文本增量</li>
 *   <li>{@code done} — 流结束标记</li>
 * </ul>
 * 思考内容由 Spring AI 的 OpenAiChatModel 解析 DashScope 兼容模式的 {@code reasoning_content}
 * 字段，存放在 {@link AssistantMessage} metadata 的 {@code reasoningContent} 键中。
 * 流式场景下每个 chunk 携带的是累计值（多轮工具调用时按轮次重置），这里换算为增量输出。
 *
 * @author javahongxi
 */
public final class ReasoningSse {

    private ReasoningSse() {
    }

    /**
     * 将 ChatResponse 流转换为 reasoning/token/done 三类 SSE 事件流
     */
    public static Flux<ServerSentEvent<String>> toSse(Flux<ChatResponse> chatResponses) {
        AtomicReference<String> lastReasoning = new AtomicReference<>("");
        return chatResponses
                .concatMap(chatResponse -> Flux.fromIterable(toEvents(chatResponse, lastReasoning)))
                .concatWith(Flux.just(event("done", "")));
    }

    /**
     * 构建错误提示事件（保持 SSE 事件协议一致，前端仍按普通回答渲染）
     */
    public static ServerSentEvent<String> errorEvent(String message) {
        return event("token", message);
    }

    /**
     * 构建流结束标记事件（错误降级等场景下手动补发）
     */
    public static ServerSentEvent<String> doneEvent() {
        return event("done", "");
    }

    /**
     * 从 ChatResponse 中提取思考内容增量与文本增量，转换为 SSE 事件
     */
    private static List<ServerSentEvent<String>> toEvents(ChatResponse chatResponse,
                                                          AtomicReference<String> lastReasoning) {
        List<ServerSentEvent<String>> events = new ArrayList<>();
        if (chatResponse == null || chatResponse.getResult() == null) {
            return events;
        }
        AssistantMessage output = chatResponse.getResult().getOutput();
        // reasoningContent 为累计值，取与上次输出的差值作为增量；
        // 多轮工具调用时新一轮的累计值会重置，此时跳过差值计算直接全量输出
        Object reasoning = output.getMetadata().get("reasoningContent");
        if (reasoning instanceof String accumulated && !accumulated.isEmpty()) {
            String last = lastReasoning.get();
            if (accumulated.length() > last.length() && accumulated.startsWith(last)) {
                events.add(event("reasoning", accumulated.substring(last.length())));
            } else if (!accumulated.equals(last)) {
                events.add(event("reasoning", accumulated));
            }
            lastReasoning.set(accumulated);
        }
        String text = output.getText();
        if (text != null && !text.isEmpty()) {
            events.add(event("token", text));
        }
        return events;
    }

    private static ServerSentEvent<String> event(String eventType, String data) {
        return ServerSentEvent.<String>builder()
                .event(eventType)
                .data(data)
                .build();
    }
}
