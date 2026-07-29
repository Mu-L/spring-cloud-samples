package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.ProjectDemoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
     * 项目演示 Agent 入口（SSE 流式输出）
     * <p>
     * 接收用户的自然语言指令，AI Agent 会自动决定调用哪些工具来完成演示任务。
     * </p>
     * <p>
     * 浏览器演示页面：http://localhost:8888，切换到「项目演示」tab 即可。
     * </p>
     *
     * @param instruction 用户的演示指令（自然语言）
     * @return Agent 的执行结果（SSE 流式）
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> demo(@RequestParam String instruction) {
        return projectDemoService.demo(instruction);
    }
}
