package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.ProjectDemoService;
import org.springframework.web.bind.annotation.*;

/**
 * 项目演示控制器
 * <p>
 * 提供 REST 接口，让 AI Agent 能够演示和验证本 Spring Cloud 项目。
 * Agent 会根据用户指令自动调用工具，完成环境检查、服务验证等操作。
 * </p>
 *
 * @author javahongxi
 */
@RestController
@RequestMapping("/ai/demo")
public class ProjectDemoController {

    private final ProjectDemoService projectDemoService;

    public ProjectDemoController(ProjectDemoService projectDemoService) {
        this.projectDemoService = projectDemoService;
    }

    /**
     * 项目演示 Agent 入口
     * <p>
     * 接收用户的自然语言指令，AI Agent 会自动决定调用哪些工具来完成演示任务。
     * </p>
     * <p>
     * 测试示例：
     * <pre>
     * # 环境检查
     * curl --get --data-urlencode "instruction=检查项目环境" "http://localhost:8888/ai/demo"
     *
     * # 检查所有服务状态
     * curl --get --data-urlencode "instruction=检查所有服务是否正常运行" "http://localhost:8888/ai/demo"
     *
     * # 验证 Web 调用链路
     * curl --get --data-urlencode "instruction=验证 Web 服务调用" "http://localhost:8888/ai/demo"
     *
     * # 验证 RPC 调用
     * curl --get --data-urlencode "instruction=验证 Dubbo 和 gRPC 调用" "http://localhost:8888/ai/demo"
     *
     * # 查看 Nacos 服务列表
     * curl --get --data-urlencode "instruction=查看 Nacos 注册了哪些服务" "http://localhost:8888/ai/demo"
     *
     * # 全面验证
     * curl --get --data-urlencode "instruction=全面验证本项目" "http://localhost:8888/ai/demo"
     *
     * # 查看项目信息
     * curl --get --data-urlencode "instruction=介绍本项目架构" "http://localhost:8888/ai/demo"
     * </pre>
     * </p>
     *
     * @param instruction 用户的演示指令（自然语言）
     * @return Agent 的执行结果
     */
    @GetMapping
    public String demo(@RequestParam String instruction) {
        return projectDemoService.demo(instruction);
    }
}
