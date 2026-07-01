package org.hongxi.cloud.sample.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 项目演示工具类
 * <p>
 * 提供 Spring Cloud 微服务项目的演示和验证能力，包括：
 * - 检查各微服务模块的健康状态
 * - 验证服务间调用链路
 * - 查看 Nacos 服务注册信息
 * - 环境检查（中间件、端口、环境变量等）
 * </p>
 * <p>
 * AI Agent 可以通过这些工具自动完成项目的环境检查和接口验证，
 * 实现"让 AI 演示本项目"的能力。
 * </p>
 *
 * @author hongxi
 */
@Component
public class ProjectDemoTools {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 服务模块定义：名称 -> 端口
     */
    private static final Map<String, Integer> SERVICES = new LinkedHashMap<>();

    static {
        SERVICES.put("nacos-discovery", 8760);
        SERVICES.put("nacos-config", 8761);
        SERVICES.put("provider-reactive", 8762);
        SERVICES.put("consumer-reactive", 8763);
        SERVICES.put("gateway", 8764);
        SERVICES.put("provider", 8765);
        SERVICES.put("consumer", 8766);
        SERVICES.put("ai", 8888);
    }

    // ==================== 服务健康检查 ====================

    /**
     * 检查指定微服务模块的健康状态
     *
     * @param moduleName 模块名称，如 provider、consumer、gateway、ai 等
     * @return 健康状态描述
     */
    @Tool(description = "检查指定微服务模块的健康状态。可用模块：nacos-discovery, nacos-config, provider, provider-reactive, consumer, consumer-reactive, gateway, ai")
    public String checkServiceHealth(
            @ToolParam(description = "模块名称，如 provider, consumer, gateway, ai, nacos-config 等") String moduleName) {
        Integer port = SERVICES.get(moduleName);
        if (port == null) {
            return "未知模块: " + moduleName + "。可用模块: " + String.join(", ", SERVICES.keySet());
        }
        return doCheckHealth(moduleName, port);
    }

    /**
     * 检查所有微服务模块的健康状态
     *
     * @return 所有模块的健康状态汇总
     */
    @Tool(description = "检查所有微服务模块的健康状态，返回每个模块的运行状态汇总")
    public String checkAllServices() {
        StringBuilder sb = new StringBuilder("=== 微服务健康检查 ===\n\n");
        int up = 0, down = 0;
        for (Map.Entry<String, Integer> entry : SERVICES.entrySet()) {
            String name = entry.getKey();
            int port = entry.getValue();
            boolean healthy = isPortOpen(port);
            String status = healthy ? "✓ 运行中" : "✗ 未运行";
            sb.append(String.format("  %-20s 端口 %-5d  %s\n", name, port, status));
            if (healthy) up++;
            else down++;
        }
        sb.append(String.format("\n共 %d 个模块，%d 个运行中，%d 个未运行", SERVICES.size(), up, down));
        return sb.toString();
    }

    // ==================== 接口验证 ====================

    /**
     * 验证 Web 服务调用链路（consumer -> provider）
     *
     * @param name 测试用的用户名
     * @return 调用结果
     */
    @Tool(description = "验证普通 Web 服务调用链路（consumer -> provider），发送请求并返回响应结果")
    public String verifyWebCall(
            @ToolParam(description = "测试用的用户名，如 hongxi") String name) {
        return doVerifyApiCall("Web 服务调用",
                "http://localhost:8766/hi?name=" + name);
    }

    /**
     * 验证 Reactive 服务调用链路（consumer-reactive -> provider-reactive）
     *
     * @param name 测试用的用户名
     * @return 调用结果
     */
    @Tool(description = "验证 Reactive Web 服务调用链路（consumer-reactive -> provider-reactive）")
    public String verifyReactiveCall(
            @ToolParam(description = "测试用的用户名，如 hongxi") String name) {
        return doVerifyApiCall("Reactive 服务调用",
                "http://localhost:8763/hi?name=" + name);
    }

    /**
     * 验证 Dubbo 服务调用链路
     *
     * @param name 测试用的用户名
     * @return 调用结果
     */
    @Tool(description = "验证 Dubbo 服务调用链路（consumer -> provider-dubbo）")
    public String verifyDubboCall(
            @ToolParam(description = "测试用的用户名，如 hongxi") String name) {
        return doVerifyApiCall("Dubbo 服务调用",
                "http://localhost:8766/dubbo?name=" + name);
    }

    /**
     * 验证 gRPC 服务调用链路
     *
     * @param name 测试用的用户名
     * @return 调用结果
     */
    @Tool(description = "验证 gRPC 服务调用链路（consumer -> grpc-server）")
    public String verifyGrpcCall(
            @ToolParam(description = "测试用的用户名，如 hongxi") String name) {
        return doVerifyApiCall("gRPC 服务调用",
                "http://localhost:8766/grpc?name=" + name);
    }

    /**
     * 验证通过网关的服务调用
     *
     * @param name 测试用的用户名
     * @return 调用结果
     */
    @Tool(description = "验证通过网关（gateway）的服务调用链路（gateway -> consumer -> provider）")
    public String verifyGatewayCall(
            @ToolParam(description = "测试用的用户名，如 hongxi") String name) {
        return doVerifyApiCall("网关路由调用",
                "http://localhost:8764/consumer-sample/hi?name=" + name);
    }

    /**
     * 验证 Nacos Config 配置管理功能
     *
     * @return 配置发布和获取的结果
     */
    @Tool(description = "验证 Nacos Config 配置管理功能：发布配置并读取验证")
    public String verifyNacosConfig() {
        try {
            // 发布配置
            String publishUrl = "http://localhost:8761/nacos/publishConfig?dataId=demo.test&content=hello-from-ai";
            String publishResult = restTemplate.getForObject(publishUrl, String.class);

            // 获取配置
            String getUrl = "http://localhost:8761/nacos/getConfig?dataId=demo.test";
            String configValue = restTemplate.getForObject(getUrl, String.class);

            if ("hello-from-ai".equals(configValue)) {
                return "✓ Nacos Config 验证通过\n  - 发布配置: dataId=demo.test, content=hello-from-ai\n  - 读取配置: " + configValue + "\n  - 配置读写一致";
            } else {
                return "✗ Nacos Config 验证失败\n  - 发布值: hello-from-ai\n  - 读取值: " + configValue;
            }
        } catch (Exception e) {
            return "✗ Nacos Config 验证异常: " + e.getMessage();
        }
    }

    // ==================== Nacos 服务发现 ====================

    /**
     * 查看 Nacos 中已注册的服务列表
     *
     * @return 已注册服务信息
     */
    @Tool(description = "查看 Nacos 注册中心中已注册的服务列表和实例信息")
    public String checkNacosServices() {
        try {
            String url = "http://localhost:8760/discovery/instances";
            String result = restTemplate.getForObject(url, String.class);
            return "=== Nacos 已注册服务 ===\n" + result;
        } catch (Exception e) {
            return "✗ 无法获取 Nacos 服务列表: " + e.getMessage() + "\n请确认 nacos-discovery 模块已启动";
        }
    }

    // ==================== 环境检查 ====================

    /**
     * 检查项目运行环境状态
     *
     * @return 环境检查结果，包括中间件、端口、环境变量等
     */
    @Tool(description = "检查项目运行环境状态，包括 Nacos、MySQL、RocketMQ、Seata Server 等中间件和端口状态")
    public String checkEnvironment() {
        StringBuilder sb = new StringBuilder("=== 环境检查 ===\n\n");

        // Nacos
        sb.append("【中间件】\n");
        sb.append("  Nacos (8848):      ").append(checkPort("127.0.0.1", 8848) ? "✓ 运行中" : "✗ 未运行").append("\n");
        sb.append("  MySQL (3306):      ").append(checkPort("127.0.0.1", 3306) ? "✓ 运行中" : "✗ 未运行").append("\n");
        sb.append("  RocketMQ (9876):   ").append(checkPort("127.0.0.1", 9876) ? "✓ 运行中" : "✗ 未运行").append("\n");
        sb.append("  Seata Server (8091): ").append(checkPort("127.0.0.1", 8091) ? "✓ 运行中" : "✗ 未运行").append("\n");

        // 环境变量
        sb.append("\n【环境变量】\n");
        sb.append("  OPENAI_API_KEY:              ").append(System.getenv("OPENAI_API_KEY") != null ? "✓ 已设置" : "✗ 未设置").append("\n");
        sb.append("  SPRING_CLOUD_NACOS_USERNAME:  ").append(System.getenv("SPRING_CLOUD_NACOS_USERNAME") != null ? "✓ 已设置" : "✗ 未设置").append("\n");
        sb.append("  SPRING_CLOUD_NACOS_PASSWORD:  ").append(System.getenv("SPRING_CLOUD_NACOS_PASSWORD") != null ? "✓ 已设置" : "✗ 未设置").append("\n");

        // Java
        sb.append("\n【Java】\n");
        sb.append("  版本: ").append(System.getProperty("java.version")).append("\n");
        sb.append("  供应商: ").append(System.getProperty("java.vendor")).append("\n");

        return sb.toString();
    }

    // ==================== 项目信息 ====================

    /**
     * 获取项目架构和模块信息
     *
     * @return 项目架构描述
     */
    @Tool(description = "获取本项目的架构信息、模块列表和技术栈说明")
    public String getProjectInfo() {
        return """
                === Spring Cloud Alibaba 示例项目 ===
                
                技术栈：Spring Boot 4.x + Spring Cloud Alibaba 2025.1.x
                
                模块列表（共 16 个）：
                
                【基础服务】
                  - cloud-nacos-discovery-sample (8760)  服务发现
                  - cloud-nacos-config-sample (8761)     配置管理
                  - cloud-gateway-sample (8764)          API 网关
                  - cloud-provider-sample (8765)         Web Provider
                  - cloud-provider-reactive-sample (8762) Reactive Provider
                  - cloud-consumer-sample (8766)         Web Consumer
                  - cloud-consumer-reactive-sample (8763) Reactive Consumer
                
                【RPC 服务】
                  - cloud-provider-dubbo-sample (50051)  Dubbo Provider
                  - cloud-grpc-server-sample (8090/9090) gRPC Server
                  - cloud-grpc-client-sample             gRPC Client
                
                【高级功能】
                  - cloud-ai-sample (8888)               Spring AI（对话/视觉/Tool Calling/Agent）
                  - cloud-stream-sample                  RocketMQ 消息流
                  - cloud-seata-sample (18081-18084)     Seata 分布式事务
                
                调用链路：
                  User -> Gateway -> Consumer -> Provider
                                   -> Provider-Reactive
                                   -> Provider-Dubbo (Triple)
                                   -> gRPC-Server
                """;
    }

    // ==================== 内部方法 ====================

    private String doCheckHealth(String name, int port) {
        if (!isPortOpen(port)) {
            return "✗ " + name + " (端口 " + port + ") 未运行";
        }
        try {
            String url = "http://localhost:" + port + "/actuator/health";
            String result = restTemplate.getForObject(url, String.class);
            if (result != null && result.contains("\"status\":\"UP\"")) {
                return "✓ " + name + " (端口 " + port + ") 健康状态: UP";
            }
            return "⚠ " + name + " (端口 " + port + ") 运行中，但健康状态异常: " + result;
        } catch (Exception e) {
            return "⚠ " + name + " (端口 " + port + ") 端口开放，但 actuator 不可访问";
        }
    }

    private String doVerifyApiCall(String callType, String url) {
        try {
            String result = restTemplate.getForObject(url, String.class);
            return "✓ " + callType + " 成功\n  请求: " + url + "\n  响应: " + result;
        } catch (Exception e) {
            return "✗ " + callType + " 失败\n  请求: " + url + "\n  错误: " + e.getMessage();
        }
    }

    private boolean isPortOpen(int port) {
        return checkPort("127.0.0.1", port);
    }

    private boolean checkPort(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
