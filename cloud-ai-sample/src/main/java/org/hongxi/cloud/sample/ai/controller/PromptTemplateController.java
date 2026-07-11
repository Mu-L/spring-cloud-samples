package org.hongxi.cloud.sample.ai.controller;

import org.hongxi.cloud.sample.ai.service.PromptTemplateService;
import org.hongxi.cloud.sample.ai.vo.CodeExplainRequest;
import org.hongxi.cloud.sample.ai.vo.CustomTemplateRequest;
import org.hongxi.cloud.sample.ai.vo.ProductDescriptionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PromptTemplate 提示词模板控制器
 * <p>
 * 提供基于模板的 LLM 调用接口，演示 PromptTemplate 的变量替换与复用能力：
 * <ul>
 *   <li>POST /ai/prompt/product    — 基于模板生成产品描述</li>
 *   <li>POST /ai/prompt/code       — 基于模板解释代码</li>
 *   <li>POST /ai/prompt/custom     — 自定义模板（通用入口）</li>
 * </ul>
 * </p>
 *
 * @author javahongxi
 */
@RestController
@RequestMapping("/ai/prompt")
public class PromptTemplateController {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateController.class);

    private final PromptTemplateService promptTemplateService;

    public PromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    /**
     * 基于模板生成产品描述
     * <p>
     * 示例请求体：
     * <pre>
     * {
     *   "product": "Spring AI 实战手册",
     *   "category": "技术书籍",
     *   "tone": "专业且幽默"
     * }
     * </pre>
     */
    @PostMapping("/product")
    public ResponseEntity<String> generateProductDescription(@RequestBody ProductDescriptionRequest request) {
        log.info("PromptTemplate 产品描述请求，product={}", request.product());
        String result = promptTemplateService.generateProductDescription(
                request.product(), request.category(), request.tone());
        return ResponseEntity.ok(result);
    }

    /**
     * 基于模板解释代码
     * <p>
     * 示例请求体：
     * <pre>
     * {
     *   "code": "public record Point(int x, int y) {}",
     *   "language": "Java",
     *   "level": "初学者"
     * }
     * </pre>
     */
    @PostMapping("/code")
    public ResponseEntity<String> explainCode(@RequestBody CodeExplainRequest request) {
        log.info("PromptTemplate 代码解释请求，language={}", request.language());
        String result = promptTemplateService.explainCode(
                request.code(), request.language(), request.level());
        return ResponseEntity.ok(result);
    }

    /**
     * 自定义模板（通用入口）
     * <p>
     * 示例请求体：
     * <pre>
     * {
     *   "template": "请用{language}写一个{function}的示例代码",
     *   "variables": {"language": "Python", "function": "快速排序"}
     * }
     * </pre>
     */
    @PostMapping("/custom")
    public ResponseEntity<String> customTemplate(@RequestBody CustomTemplateRequest request) {
        log.info("PromptTemplate 自定义模板请求，variables={}", request.variables().keySet());
        String result = promptTemplateService.customTemplate(request.template(), request.variables());
        return ResponseEntity.ok(result);
    }
}
