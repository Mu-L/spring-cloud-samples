package org.hongxi.cloud.sample.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
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

    public record SendResult(String topic, String message, boolean success) {}
}
