package org.hongxi.cloud.grpc.discovery;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.StatusOr;

/**
 * 基于 Spring Cloud {@link DiscoveryClient} 的 gRPC NameResolver 实现。
 * <p>
 * 将 gRPC 的服务发现桥接到 Spring Cloud 的服务注册中心（如 Nacos），
 * 使得 gRPC 客户端可以通过逻辑服务名（如 discovery:///grpc-server-sample）
 * 自动发现并负载均衡后端实例。
 *
 * @author hongxi
 */
public class DiscoveryClientNameResolver extends NameResolver {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryClientNameResolver.class);

    private final String serviceName;
    private final DiscoveryClient discoveryClient;
    private final Executor offloadExecutor;
    private Listener2 listener;
    private List<ServiceInstance> previousInstances = List.of();

    public DiscoveryClientNameResolver(String serviceName,
                                       DiscoveryClient discoveryClient,
                                       Args args) {
        this.serviceName = serviceName;
        this.discoveryClient = discoveryClient;
        this.offloadExecutor = args.getOffloadExecutor();
    }

    @Override
    public String getServiceAuthority() {
        return serviceName;
    }

    @Override
    public void start(Listener2 listener) {
        this.listener = listener;
        resolve();
    }

    @Override
    public void refresh() {
        resolve();
    }

    @Override
    public void shutdown() {
        listener = null;
        previousInstances = List.of();
    }

    /**
     * 执行服务发现：从 Spring Cloud DiscoveryClient 获取实例列表，
     * 转换为 gRPC 的 EquivalentAddressGroup 并通知 listener。
     */
    private void resolve() {
        if (listener == null) {
            return;
        }
        // 在 offload executor 上执行，避免阻塞 gRPC 的 SynchronizationContext
        if (offloadExecutor != null) {
            offloadExecutor.execute(this::doResolve);
        } else {
            doResolve();
        }
    }

    private void doResolve() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances.isEmpty()) {
                log.warn("[gRPC Discovery] No instances found for service: {}", serviceName);
                listener.onError(Status.UNAVAILABLE
                        .withDescription("No instances found for service: " + serviceName));
                return;
            }

            // 检查实例是否有变化
            if (!hasChanged(instances)) {
                log.debug("[gRPC Discovery] No changes for service: {}", serviceName);
                return;
            }

            // 转换为 gRPC 地址组
            List<EquivalentAddressGroup> addressGroups = instances.stream()
                    .map(instance -> {
                        String host = instance.getHost();
                        int port = instance.getPort();
                        log.debug("[gRPC Discovery] Found instance {}:{} for service {}",
                                host, port, serviceName);
                        return new EquivalentAddressGroup(new InetSocketAddress(host, port));
                    })
                    .toList();

            previousInstances = instances;

            // 通知 gRPC 更新地址列表
            listener.onResult(ResolutionResult.newBuilder()
                    .setAddressesOrError(StatusOr.fromValue(addressGroups))
                    .build());

            log.info("[gRPC Discovery] Updated {} instances for service {}",
                    addressGroups.size(), serviceName);

        } catch (Exception e) {
            log.error("[gRPC Discovery] Failed to resolve service: {}", serviceName, e);
            listener.onError(Status.UNAVAILABLE
                    .withCause(e)
                    .withDescription("Failed to resolve service: " + serviceName));
        }
    }

    /**
     * 检查服务实例列表是否有变化（基于 host:port 比较）
     */
    private boolean hasChanged(List<ServiceInstance> newInstances) {
        if (previousInstances.size() != newInstances.size()) {
            return true;
        }
        for (ServiceInstance instance : newInstances) {
            boolean found = previousInstances.stream()
                    .anyMatch(prev -> prev.getHost().equals(instance.getHost())
                            && prev.getPort() == instance.getPort());
            if (!found) {
                return true;
            }
        }
        return false;
    }
}
