package org.hongxi.cloud.sample.grpc.server;

import io.grpc.stub.StreamObserver;
import org.hongxi.cloud.sample.idl.stream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

/**
 * gRPC 流式服务实现，演示三种流式 RPC 模式：
 * <ul>
 *   <li>Server Streaming — 服务端流式返回斐波那契数列</li>
 *   <li>Client Streaming — 客户端流式发送数字，服务端汇总</li>
 *   <li>Bidirectional Streaming — 双向流式交互</li>
 * </ul>
 */
@GrpcService
public class StreamingServiceImpl extends StreamingServiceGrpc.StreamingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(StreamingServiceImpl.class);

    /**
     * Server Streaming：根据 limit 流式返回斐波那契数列。
     * <p>
     * 客户端发送一个上限值，服务端逐个计算并推送斐波那契数，直到值超过 limit。
     */
    @Override
    public void fibonacci(FibonacciRequest request, StreamObserver<FibonacciReply> responseObserver) {
        int limit = request.getLimit();
        log.info("[Server Streaming] fibonacci request, limit={}", limit);

        long a = 0, b = 1;
        int count = 0;
        while (a <= limit) {
            responseObserver.onNext(FibonacciReply.newBuilder().setValue(a).build());
            log.debug("[Server Streaming] fibonacci -> {}", a);
            long next = a + b;
            a = b;
            b = next;
            count++;
        }

        responseObserver.onCompleted();
        log.info("[Server Streaming] fibonacci completed, count={}", count);
    }

    /**
     * Client Streaming：客户端流式发送多个数字，服务端汇总计算总和与平均值。
     * <p>
     * 服务端通过 {@link StreamObserver} 接收每个客户端消息，在 onCompleted 时返回汇总结果。
     */
    @Override
    public StreamObserver<AccumulateRequest> accumulate(StreamObserver<AccumulateReply> responseObserver) {
        log.info("[Client Streaming] accumulate started");

        return new StreamObserver<AccumulateRequest>() {
            double sum = 0;
            int count = 0;

            @Override
            public void onNext(AccumulateRequest request) {
                sum += request.getValue();
                count++;
                log.debug("[Client Streaming] received value={}, running sum={}", request.getValue(), sum);
            }

            @Override
            public void onError(Throwable t) {
                log.error("[Client Streaming] accumulate error", t);
            }

            @Override
            public void onCompleted() {
                double average = count > 0 ? sum / count : 0;
                AccumulateReply reply = AccumulateReply.newBuilder()
                        .setSum(sum)
                        .setAverage(average)
                        .setCount(count)
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
                log.info("[Client Streaming] accumulate completed, count={}, sum={}, average={}",
                        count, sum, average);
            }
        };
    }

    /**
     * Bidirectional Streaming：双向流式交互。
     * <p>
     * 客户端流式发送名称，服务端对每个名称生成带序号的问候语并实时推送。
     */
    @Override
    public StreamObserver<ChatRequest> chat(StreamObserver<ChatReply> responseObserver) {
        log.info("[BiDi Streaming] chat started");

        return new StreamObserver<ChatRequest>() {
            long sequence = 0;

            @Override
            public void onNext(ChatRequest request) {
                sequence++;
                String greeting = "Hello, " + request.getName() + "! (msg #" + sequence + ")";
                responseObserver.onNext(ChatReply.newBuilder()
                        .setGreeting(greeting)
                        .setSequence(sequence)
                        .build());
                log.debug("[BiDi Streaming] chat: {} -> {}", request.getName(), greeting);
            }

            @Override
            public void onError(Throwable t) {
                log.error("[BiDi Streaming] chat error", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
                log.info("[BiDi Streaming] chat completed, total messages={}", sequence);
            }
        };
    }
}
