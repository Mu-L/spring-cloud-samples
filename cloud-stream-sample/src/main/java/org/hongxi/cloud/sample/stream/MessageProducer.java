package org.hongxi.cloud.sample.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * Created by shenhongxi on 2020/7/12.
 */
@Slf4j
@Component
public class MessageProducer {

    @Autowired
    private StreamBridge streamBridge;

    public void send(String message) {
        boolean result = streamBridge.send("output-out-0", message);
        log.info("Send message: {}, result: {}", message, result);
    }
}
