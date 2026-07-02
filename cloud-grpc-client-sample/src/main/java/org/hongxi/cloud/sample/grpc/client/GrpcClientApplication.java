package org.hongxi.cloud.sample.grpc.client;

import io.grpc.NameResolverRegistry;
import io.grpc.stub.StreamObserver;
import org.hongxi.cloud.grpc.discovery.DiscoveryClientNameResolver;
import org.hongxi.cloud.grpc.discovery.DiscoveryClientNameResolverProvider;
import org.hongxi.cloud.sample.idl.stream.*;
import org.hongxi.cloud.sample.idl.unary.GreeterGrpc;
import org.hongxi.cloud.sample.idl.unary.GreeterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcClientFactory;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.client.SimpleStubFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 客户端示例应用 — 演示 gRPC 四种调用模式。
 *
 * <h2>gRPC 在 Spring Cloud 体系中的服务发现</h2>
 * <p>
 * gRPC 原生通过 {@code NameResolver} SPI 机制解析服务地址（如 DNS、static 等），
 * 而 Spring Cloud 通过 {@link DiscoveryClient} 抽象了服务注册中心（Nacos、Eureka、Consul 等）。
 * 两者的桥接方式如下：
 *
 * <pre>
 * 配置层（application.yml）
 *   spring.grpc.client.channel.default.target: discovery:///grpc-server-sample
 *                                              ↑ scheme="discovery"  ↑ 服务名
 *
 * 运行时调用链
 *   gRPC Channel 创建
 *     → NameResolverRegistry 查找 scheme="discovery" 的 NameResolverProvider
 *     → DiscoveryClientNameResolverProvider（priority=6，高于 DNS 的 5）
 *     → 创建 DiscoveryClientNameResolver
 *     → 调用 DiscoveryClient.getInstances("grpc-server-sample")
 *     → 从注册中心（如 Nacos）获取实例列表（host:port）
 *     → 构建 EquivalentAddressGroup 供 gRPC Channel 使用
 * </pre>
 *
 * <h3>关键组件</h3>
 * <ul>
 *   <li>{@link DiscoveryClientNameResolverProvider} — 识别 {@code discovery:///} scheme，创建对应 NameResolver</li>
 *   <li>{@link DiscoveryClientNameResolver} — 调用 Spring Cloud {@link DiscoveryClient} 解析服务实例，
 *       并支持实例变更时自动刷新</li>
 *   <li>{@code @ImportGrpcClients} — 扫描 proto 生成的 stub 接口，注册为 Spring Bean</li>
 * </ul>
 *
 * <h3>本示例演示的四种 gRPC 调用模式</h3>
 * <ol>
 *   <li><b>Unary</b> — 客户端发送单个请求，服务端返回单个响应</li>
 *   <li><b>Server Streaming</b> — 客户端发送单个请求，服务端流式返回多个响应（斐波那契数列）</li>
 *   <li><b>Client Streaming</b> — 客户端流式发送多个请求，服务端汇总后返回单个响应（累加求均值）</li>
 *   <li><b>Bidirectional Streaming</b> — 客户端和服务端双向流式交互（实时聊天）</li>
 * </ol>
 *
 * @see DiscoveryClientNameResolverProvider
 * @see DiscoveryClientNameResolver
 */
@SpringBootApplication
@ImportGrpcClients(basePackages = {
        "org.hongxi.cloud.sample.idl.unary",
        "org.hongxi.cloud.sample.idl.stream"
})
public class GrpcClientApplication {
    private static final Logger log = LoggerFactory.getLogger(GrpcClientApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }

    /**
     * 将 NameResolverProvider 注册到 gRPC 全局注册表。
     * <p>
     * 必须确保 nameResolverProvider 先于 stub bean 实例化
     * <p>
     */
    @Bean
    DiscoveryClientNameResolverProvider nameResolverProvider(DiscoveryClient discoveryClient) {
        var provider = new DiscoveryClientNameResolverProvider(discoveryClient);
        NameResolverRegistry.getDefaultRegistry().register(provider);
        return provider;
    }

    /**
     * 手动注册异步 gRPC Stub Bean。
     * <p>
     * {@code @ImportGrpcClients} 默认使用 {@code BlockingStubFactory} 扫描包路径，
     * 只会注册 BlockingStub 类型的 Bean。Client Streaming / BiDi Streaming
     * 所需的异步 {@link StreamingServiceGrpc.StreamingServiceStub}
     * （继承 {@code AbstractAsyncStub}）不会被自动注册，需要手动声明。
     */
    @Bean
    StreamingServiceGrpc.StreamingServiceStub streamingAsyncStub(GrpcClientFactory grpcClientFactory) {
        return grpcClientFactory.getClient("default",
                StreamingServiceGrpc.StreamingServiceStub.class,
                SimpleStubFactory.class);
    }

    /**
     * 演示 gRPC 四种调用模式：Unary / Server Streaming / Client Streaming / BiDi Streaming
     */
    @Bean
    CommandLineRunner runner(GreeterGrpc.GreeterBlockingStub greeterStub,
                             StreamingServiceGrpc.StreamingServiceBlockingStub streamingBlockingStub,
                             StreamingServiceGrpc.StreamingServiceStub streamingAsyncStub) {
        return args -> {
            // ==================== 1. Unary ====================
            demoUnary(greeterStub);

            // ==================== 2. Server Streaming ====================
            demoServerStreaming(streamingBlockingStub);

            // ==================== 3. Client Streaming ====================
            demoClientStreaming(streamingAsyncStub);

            // ==================== 4. Bidirectional Streaming ====================
            demoBidiStreaming(streamingAsyncStub);
        };
    }

    /**
     * 1. Unary RPC：客户端发送单个请求，服务端返回单个响应。
     */
    private void demoUnary(GreeterGrpc.GreeterBlockingStub stub) {
        log.info("===== 1. Unary RPC =====");
        GreeterRequest request = GreeterRequest.newBuilder().setName("lily").build();
        var reply = stub.greet(request);
        log.info("Unary result: {}", reply.getMessage());
    }

    /**
     * 2. Server Streaming RPC：客户端发送上限值，服务端流式返回斐波那契数列。
     */
    private void demoServerStreaming(StreamingServiceGrpc.StreamingServiceBlockingStub stub) {
        log.info("===== 2. Server Streaming RPC =====");
        FibonacciRequest request = FibonacciRequest.newBuilder().setLimit(100).build();
        Iterator<FibonacciReply> iterator = stub.fibonacci(request);

        log.info("Fibonacci numbers up to 100:");
        while (iterator.hasNext()) {
            log.info("  -> {}", iterator.next().getValue());
        }
    }

    /**
     * 3. Client Streaming RPC：客户端流式发送多个数字，服务端汇总返回总和与平均值。
     */
    private void demoClientStreaming(StreamingServiceGrpc.StreamingServiceStub stub) throws Exception {
        log.info("===== 3. Client Streaming RPC =====");
        CountDownLatch latch = new CountDownLatch(1);
        final AccumulateReply[] result = new AccumulateReply[1];

        var requestObserver = stub.accumulate(new StreamObserver<>() {
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

        // 流式发送 5 个数字
        List<Double> values = List.of(10.0, 20.0, 30.0, 40.0, 50.0);
        for (double v : values) {
            requestObserver.onNext(AccumulateRequest.newBuilder().setValue(v).build());
            log.info("  Sent: {}", v);
        }
        requestObserver.onCompleted();

        latch.await(5, TimeUnit.SECONDS);
        if (result[0] != null) {
            log.info("Accumulate result: count={}, sum={}, average={}",
                    result[0].getCount(), result[0].getSum(), result[0].getAverage());
        }
    }

    /**
     * 4. Bidirectional Streaming RPC：双向流式聊天。
     */
    private void demoBidiStreaming(StreamingServiceGrpc.StreamingServiceStub stub) throws Exception {
        log.info("===== 4. Bidirectional Streaming RPC =====");
        CountDownLatch latch = new CountDownLatch(1);

        var responseObserver = new StreamObserver<ChatReply>() {
            @Override
            public void onNext(ChatReply reply) {
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

        var requestObserver = stub.chat(responseObserver);

        // 流式发送多个名称
        List<String> names = List.of("Alice", "Bob", "Charlie");
        for (String name : names) {
            requestObserver.onNext(ChatRequest.newBuilder().setName(name).build());
            log.info("  Sent: {}", name);
        }
        requestObserver.onCompleted();

        latch.await(5, TimeUnit.SECONDS);
        log.info("BiDi streaming completed");
    }
}
