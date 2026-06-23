package org.hongxi.cloud.sample.grpc.client;

import io.grpc.NameResolverRegistry;
import org.hongxi.cloud.grpc.discovery.DiscoveryClientNameResolver;
import org.hongxi.cloud.grpc.discovery.DiscoveryClientNameResolverProvider;
import org.hongxi.cloud.sample.idl.unary.GreeterGrpc;
import org.hongxi.cloud.sample.idl.unary.GreeterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.ImportGrpcClients;

/**
 * gRPC 客户端示例应用。
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
 * <h3>时序保证</h3>
 * <p>
 * {@link #nameResolverProvider} 必须先于 stub bean 实例化，
 * 以保证 provider 在 stub 实例化前注册到 gRPC 全局 {@link NameResolverRegistry}。
 *
 * @see DiscoveryClientNameResolverProvider
 * @see DiscoveryClientNameResolver
 */
@SpringBootApplication
@ImportGrpcClients(basePackages = "org.hongxi.cloud.sample.idl.unary")
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

    @Bean
    CommandLineRunner runner(GreeterGrpc.GreeterBlockingStub stub) {
        return args -> {
            GreeterRequest request = GreeterRequest.newBuilder().setName("lily").build();
            log.info("Calling gRPC service, result: {}", stub.greet(request));
        };
    }
}
