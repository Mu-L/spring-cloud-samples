package org.hongxi.cloud.grpc.discovery;

import java.net.URI;

import org.springframework.cloud.client.discovery.DiscoveryClient;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

/**
 * gRPC NameResolverProvider，支持 "discovery" scheme。
 * <p>
 * 当 gRPC 客户端配置 target 为 "discovery:///service-name" 时，
 * 本 Provider 会创建 {@link DiscoveryClientNameResolver} 实例，
 * 通过 Spring Cloud DiscoveryClient 解析服务地址。
 * <p>
 * priority 设为 6，高于 DNS 的默认优先级 5，确保 "discovery" scheme 优先被使用。
 *
 * @author hongxi
 */
public class DiscoveryClientNameResolverProvider extends NameResolverProvider {

    public static final String DISCOVERY_SCHEME = "discovery";

    private final DiscoveryClient discoveryClient;

    public DiscoveryClientNameResolverProvider(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        if (!DISCOVERY_SCHEME.equals(targetUri.getScheme())) {
            return null;
        }
        // discovery:///grpc-server-sample → path = "/grpc-server-sample"
        String serviceName = targetUri.getPath();
        if (serviceName == null || serviceName.length() <= 1 || !serviceName.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Incorrectly formatted target uri; " +
                    "expected: 'discovery:[//]/<service-name>'; " +
                    "but was '" + targetUri + "'");
        }
        // 去掉前导 "/"
        return new DiscoveryClientNameResolver(serviceName.substring(1), discoveryClient, args);
    }

    @Override
    public String getDefaultScheme() {
        return DISCOVERY_SCHEME;
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 6; // 高于 DNS (5)
    }
}
