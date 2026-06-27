package org.hongxi.cloud.sample.ai.service;

import org.hongxi.cloud.sample.ai.tool.ProjectDemoTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 项目演示 Agent 服务
 * <p>
 * 基于 ReAct Agent 模式，让 AI 具备演示本项目的能力。
 * Agent 会根据用户的指令，自主决定调用哪些工具来完成环境检查、服务验证等操作。
 * </p>
 * <p>
 * 支持的演示场景：
 * - 环境检查：检查 Nacos、MySQL、RocketMQ、Seata 等中间件状态
 * - 服务健康检查：检查各微服务模块的运行状态
 * - 接口验证：验证 Web/Reactive/Dubbo/gRPC 服务调用链路
 * - 网关路由：验证通过 Gateway 的服务调用
 * - 配置管理：验证 Nacos Config 配置发布和读取
 * - 服务发现：查看 Nacos 中已注册的服务列表
 * </p>
 *
 * @author hongxi
 */
@Service
public class ProjectDemoService {

    private static final Logger log = LoggerFactory.getLogger(ProjectDemoService.class);

    private final ChatClient chatClient;
    private final ProjectDemoTools projectDemoTools;

    public ProjectDemoService(ChatClient.Builder builder, ProjectDemoTools projectDemoTools) {
        this.chatClient = builder.build();
        this.projectDemoTools = projectDemoTools;
    }

    /**
     * 项目演示 Agent
     * <p>
     * AI 会根据用户指令自动调用工具完成演示任务。
     * </p>
     * <p>
     * 测试示例：
     * - "检查项目环境"
     * - "检查所有服务是否正常运行"
     * - "验证 Web 服务调用链路"
     * - "验证 Dubbo 和 gRPC 调用"
     * - "查看 Nacos 中注册了哪些服务"
     * - "演示 Nacos Config 配置管理"
     * - "全面验证本项目"
     * </p>
     *
     * @param instruction 用户的演示指令
     * @return Agent 的执行结果
     */
    public Map<String, Object> demo(String instruction) {
        log.info("项目演示 Agent 收到指令: {}", instruction);

        String response = chatClient.prompt()
                .system("""
                        你是 Spring Cloud Alibaba 示例项目的演示助手，负责帮助用户演示和验证本项目。
                        
                        本项目是基于 Spring Boot 4.x + Spring Cloud Alibaba 2025.1.x 的微服务示例项目，
                        包含 16 个模块，涵盖服务发现、配置管理、网关、RPC（Dubbo/gRPC）、
                        Spring AI、消息流（Stream）、分布式事务（Seata）等功能。
                        
                        你可以使用以下工具：
                        - checkServiceHealth: 检查单个微服务模块的健康状态
                        - checkAllServices: 检查所有微服务模块的运行状态
                        - verifyWebCall: 验证普通 Web 服务调用链路
                        - verifyReactiveCall: 验证 Reactive 服务调用链路
                        - verifyDubboCall: 验证 Dubbo 服务调用链路
                        - verifyGrpcCall: 验证 gRPC 服务调用链路
                        - verifyGatewayCall: 验证通过网关的服务调用
                        - verifyNacosConfig: 验证 Nacos Config 配置管理
                        - checkNacosServices: 查看 Nacos 注册中心的服务列表
                        - checkEnvironment: 检查运行环境（中间件、端口、环境变量）
                        - getProjectInfo: 获取项目架构和模块信息
                        
                        执行策略：
                        1. 收到指令后，分析需要调用哪些工具
                        2. 按依赖顺序调用工具（如先检查服务是否运行，再验证接口）
                        3. 根据工具返回结果给出清晰的汇总报告
                        4. 如果某个服务未运行，提醒用户需要先启动
                        
                        报告格式要求：
                        - 使用简洁的表格或列表展示结果
                        - 用 ✓/✗ 标记成功/失败
                        - 如有异常，给出具体的错误信息和建议
                        """)
                .user(instruction)
                .tools(projectDemoTools)
                .call()
                .content();

        log.info("项目演示 Agent 完成");

        Map<String, Object> result = new HashMap<>();
        result.put("instruction", instruction);
        result.put("result", response);
        result.put("type", "project-demo");
        return result;
    }
}
