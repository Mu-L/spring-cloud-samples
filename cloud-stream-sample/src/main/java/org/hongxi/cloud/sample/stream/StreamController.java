package org.hongxi.cloud.sample.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stream 消息发布控制器
 * <p>
 * 演示通过 REST API 交互式地向不同 Topic 发送消息，
 * 展示 StreamBridge 的编程式消息发布能力。
 *
 * @author hongxi
 */
@RestController
@RequestMapping("/stream")
public class StreamController {

    private static final Logger log = LoggerFactory.getLogger(StreamController.class);

    private final StreamBridge streamBridge;

    public StreamController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    /**
     * 发送消息到 transform 管道（观察消息被大写转换后输出到 stream-demo-topic2）
     * <p>
     * 消息流: REST API → transformPublish-out-0 → [binder] → stream-transform-topic → transform-in-0 → [toUpperCase] → transform-out-0 → stream-demo-topic2
     */
    @PostMapping("/send")
    public SendResult send(@RequestParam(defaultValue = "hello spring cloud stream") String message) {
        boolean result = streamBridge.send("transformPublish-out-0", message);
        log.info("发送消息到 transform 管道: {}, 结果: {}", message, result);
        return new SendResult("transformPublish-out-0", message, result);
    }

    /**
     * 发送延迟消息到 stream-delay-topic
     * <p>
     * 通过 Message header 设置 RocketMQ 延迟级别（DELAY_TIME_LEVEL），消息经 binder 投递到 delay topic，
     * 在指定延迟后被消费者接收。支持延迟级别：
     * 1=1s, 2=5s, 3=10s, 4=30s, 5=1m, 6=2m, 7=3m, 8=4m, 9=5m, 10=6m, 11=7m, 12=8m,
     * 13=9m, 14=10m, 15=20m, 16=30m, 17=1h, 18=2h
     * <p>
     * 观察日志中消息的接收时间与发送时间的差值，验证延迟效果。
     *
     * @param message    消息内容
     * @param delayLevel 延迟级别 (1-18)，默认 2 (5秒)
     */
    @PostMapping("/delay")
    public SendResult sendDelay(
            @RequestParam(defaultValue = "delayed message") String message,
            @RequestParam(defaultValue = "2") int delayLevel) {
        // RocketMQ 延迟级别 header：DELAY（对应 MessageConst.PROPERTY_DELAY_TIME_LEVEL = "DELAY"）
        Message<String> msg = MessageBuilder.withPayload(message)
                .setHeader("DELAY", delayLevel)
                .build();
        boolean result = streamBridge.send("delayPublish-out-0", msg);
        log.info("发送延迟消息: {}, 延迟级别: {} (约{}秒后投递), 结果: {}", message, delayLevel, delayLevelToSeconds(delayLevel), result);
        return new SendResult("stream-delay-topic", message + " (delayLevel=" + delayLevel + ")", result);
    }

    /**
     * 发送顺序消息到 stream-fifo-topic
     * <p>
     * 通过 ORDER_KEY header 指定分区键，相同 ORDER_KEY 的消息路由到同一队列，
     * 配合 producer.orderly=true 和 consumer.orderly=true 保证消息按发送顺序被消费。
     *
     * @param message  消息内容
     * @param orderKey 分区键（相同 orderKey 的消息保证顺序）
     */
    @PostMapping("/fifo")
    public SendResult sendFifo(
            @RequestParam(defaultValue = "ordered message") String message,
            @RequestParam(defaultValue = "order-1") String orderKey) {
        Message<String> msg = MessageBuilder.withPayload(message)
                .setHeader("ORDER_KEY", orderKey)
                .build();
        boolean result = streamBridge.send("fifoPublish-out-0", msg);
        log.info("发送顺序消息: {}, orderKey: {}, 结果: {}", message, orderKey, result);
        return new SendResult("stream-fifo-topic", message + " (orderKey=" + orderKey + ")", result);
    }

    /**
     * 将 RocketMQ 延迟级别转换为近似秒数（用于日志提示）
     */
    private int delayLevelToSeconds(int level) {
        return switch (level) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 30;
            case 5 -> 60;
            case 6 -> 120;
            case 7 -> 180;
            case 8 -> 240;
            case 9 -> 300;
            case 10 -> 360;
            case 11 -> 420;
            case 12 -> 480;
            case 13 -> 540;
            case 14 -> 600;
            case 15 -> 1200;
            case 16 -> 1800;
            case 17 -> 3600;
            case 18 -> 7200;
            default -> level;
        };
    }

    public record SendResult(String topic, String message, boolean success) {}
}
