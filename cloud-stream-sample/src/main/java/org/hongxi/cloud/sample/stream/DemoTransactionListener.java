package org.hongxi.cloud.sample.stream;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 事务消息监听器 - 演示 RocketMQ 事务消息的两阶段提交
 * <p>
 * 事务消息流程：
 * <ol>
 *   <li>发送半消息（Half Message）到 Broker，此时消费者不可见</li>
 *   <li>执行 {@link #executeLocalTransaction} 本地事务逻辑</li>
 *   <li>根据返回状态 Commit/Rollback 消息</li>
 *   <li>若状态为 Unknown，Broker 定期回查 {@link #checkLocalTransaction}</li>
 * </ol>
 * <p>
 * 事务结果决策逻辑（优先级从高到低）：
 * <ol>
 *   <li>消息 header 中的 TX_ARG：commit → 提交，rollback → 回滚</li>
 *   <li>方法参数 arg：同上</li>
 *   <li>以上均无时随机决定，模拟 commit / rollback 两种场景</li>
 * </ol>
 *
 * @author javahongxi
 */
@Component("demoTransactionListener")
public class DemoTransactionListener implements TransactionListener {

    private static final Logger log = LoggerFactory.getLogger(DemoTransactionListener.class);

    private final AtomicInteger checkCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, LocalTransactionState> txStatusMap = new ConcurrentHashMap<>();

    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
        // 优先从消息属性读取 TX_ARG，其次使用方法参数 arg
        String txArg = msg.getProperty("TX_ARG");
        if (txArg == null && arg != null) {
            txArg = arg.toString();
        }
        log.info("[事务消息] 执行本地事务: body={}, arg={}", body, txArg != null ? txArg : "null");

        // 根据 arg 明确决定事务结果
        if ("commit".equalsIgnoreCase(txArg)) {
            log.info("[事务消息] 本地事务提交 (arg=commit)");
            return LocalTransactionState.COMMIT_MESSAGE;
        } else if ("rollback".equalsIgnoreCase(txArg)) {
            log.info("[事务消息] 本地事务回滚 (arg=rollback)");
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }

        // 无 arg 时随机决定，模拟 commit / rollback 两种场景
        boolean commit = ThreadLocalRandom.current().nextBoolean();
        if (commit) {
            log.info("[事务消息] 本地事务提交 (随机)");
            return LocalTransactionState.COMMIT_MESSAGE;
        }
        log.info("[事务消息] 本地事务回滚 (随机)");
        return LocalTransactionState.ROLLBACK_MESSAGE;
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        int count = checkCount.incrementAndGet();
        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
        log.info("[事务消息] 事务回查 #{}: msgId={}, body={}", count, msg.getMsgId(), body);

        // 默认返回 COMMIT，实际场景中应查询本地事务状态
        return LocalTransactionState.COMMIT_MESSAGE;
    }
}
