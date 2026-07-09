package org.hongxi.cloud.sample.consumer.controller;

import io.grpc.stub.StreamObserver;
import org.hongxi.cloud.sample.idl.stream.*;
import org.hongxi.cloud.sample.idl.unary.GreeterGrpc;
import org.hongxi.cloud.sample.idl.unary.GreeterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 四种调用模式演示控制器。
 *
 * <p>
 *     Unary / Server Streaming / Client Streaming / BiDi Streaming
 * </p>
 *
 * @author javahongxi
 */
@DependsOn("nameResolverProvider")
@RestController
@RequestMapping("/grpc")
public class GrpcController {

    private static final Logger log = LoggerFactory.getLogger(GrpcController.class);

    @Autowired
    private GreeterGrpc.GreeterBlockingStub greeterStub;

    @Autowired
    private StreamingServiceGrpc.StreamingServiceBlockingStub streamingBlockingStub;

    @Autowired
    private StreamingServiceGrpc.StreamingServiceStub streamingAsyncStub;

    /**
     * 1. Unary RPC：客户端发送单个请求，服务端返回单个响应。
     *
     * @param name 用户名
     * @return 问候结果
     */
    @GetMapping("/unary")
    public Map<String, Object> unary(@RequestParam(defaultValue = "lily") String name) {
        log.info("===== Unary RPC, name={} =====", name);
        GreeterRequest request = GreeterRequest.newBuilder().setName(name).build();
        var reply = greeterStub.greet(request);
        String message = reply.getMessage();
        log.info("Unary result: {}", message);
        return Map.of("mode", "Unary", "result", message);
    }

    /**
     * 2. Server Streaming RPC：客户端发送上限值，服务端流式返回斐波那契数列。
     *
     * @param limit 斐波那契数列上限
     * @return 斐波那契数列列表
     */
    @GetMapping("/server-streaming")
    public Map<String, Object> serverStreaming(@RequestParam(defaultValue = "100") int limit) {
        log.info("===== Server Streaming RPC, limit={} =====", limit);
        FibonacciRequest request = FibonacciRequest.newBuilder().setLimit(limit).build();
        Iterator<FibonacciReply> iterator = streamingBlockingStub.fibonacci(request);

        List<Long> values = new ArrayList<>();
        while (iterator.hasNext()) {
            long v = iterator.next().getValue();
            values.add(v);
        }
        log.info("Fibonacci numbers up to {}: {}", limit, values);
        return Map.of("mode", "ServerStreaming", "limit", limit, "values", values);
    }

    /**
     * 3. Client Streaming RPC：客户端流式发送多个数字，服务端汇总返回总和与平均值。
     *
     * @param values 逗号分隔的数字列表，默认 10,20,30,40,50
     * @return 汇总结果
     */
    @PostMapping("/client-streaming")
    public Map<String, Object> clientStreaming(
            @RequestParam(defaultValue = "10,20,30,40,50") String values) throws Exception {
        log.info("===== Client Streaming RPC, values={} =====", values);
        CountDownLatch latch = new CountDownLatch(1);
        final AccumulateReply[] result = new AccumulateReply[1];

        var requestObserver = streamingAsyncStub.accumulate(new StreamObserver<>() {
            @Override
            public void onNext(AccumulateReply reply) {
                result[0] = reply;
            }

            @Override
            public void onError(Throwable t) {
                log.error("Client Streaming error", t);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        // 解析参数并流式发送
        List<Double> nums = new ArrayList<>();
        for (String v : values.split(",")) {
            double d = Double.parseDouble(v.trim());
            nums.add(d);
            requestObserver.onNext(AccumulateRequest.newBuilder().setValue(d).build());
            log.info("  Sent: {}", d);
        }
        requestObserver.onCompleted();

        latch.await(5, TimeUnit.SECONDS);
        if (result[0] != null) {
            log.info("Accumulate result: count={}, sum={}, average={}",
                    result[0].getCount(), result[0].getSum(), result[0].getAverage());
            return Map.of("mode", "ClientStreaming",
                    "count", result[0].getCount(),
                    "sum", result[0].getSum(),
                    "average", result[0].getAverage());
        }
        return Map.of("mode", "ClientStreaming", "error", "No response received");
    }

    /**
     * 4. Bidirectional Streaming RPC：双向流式聊天。
     *
     * @param names 逗号分隔的名称列表，默认 Alice,Bob,Charlie
     * @return 聊天回复列表
     */
    @PostMapping("/bidi-streaming")
    public Map<String, Object> bidiStreaming(
            @RequestParam(defaultValue = "Alice,Bob,Charlie") String names) throws Exception {
        log.info("===== Bidirectional Streaming RPC, names={} =====", names);
        CountDownLatch latch = new CountDownLatch(1);
        List<String> greetings = Collections.synchronizedList(new ArrayList<>());

        var responseObserver = new StreamObserver<ChatReply>() {
            @Override
            public void onNext(ChatReply reply) {
                greetings.add(reply.getGreeting());
                log.info("  Received: {}", reply.getGreeting());
            }

            @Override
            public void onError(Throwable t) {
                log.error("BiDi Streaming error", t);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        var requestObserver = streamingAsyncStub.chat(responseObserver);

        // 流式发送名称
        for (String name : names.split(",")) {
            String trimmed = name.trim();
            requestObserver.onNext(ChatRequest.newBuilder().setName(trimmed).build());
            log.info("  Sent: {}", trimmed);
        }
        requestObserver.onCompleted();

        latch.await(5, TimeUnit.SECONDS);
        log.info("BiDi streaming completed, greetings: {}", greetings);
        return Map.of("mode", "BidirectionalStreaming", "greetings", greetings);
    }
}
