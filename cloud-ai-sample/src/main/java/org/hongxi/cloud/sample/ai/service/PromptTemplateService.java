package org.hongxi.cloud.sample.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * PromptTemplate 提示词模板服务
 * <p>
 * 演示 Spring AI 的提示词模板能力：
 * <ul>
 *   <li>使用 {@link PromptTemplate} 进行变量占位符替换（{variable} 语法）</li>
 *   <li>支持从字符串模板动态生成结构化 Prompt</li>
 *   <li>将渲染后的 Prompt 交给 ChatClient 调用 LLM</li>
 * </ul>
 * 相比硬编码字符串拼接，PromptTemplate 提供了更好的可读性、可维护性和复用性。
 * </p>
 *
 * @author javahongxi
 */
@Service
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);

    private final ChatClient chatClient;

    public PromptTemplateService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 基于模板生成产品描述
     * <p>
     * 模板变量：{product}、{category}、{tone}
     * </p>
     *
     * @param product  产品名称
     * @param category 产品类别
     * @param tone     文案风格（如"专业"、"幽默"、"简洁"）
     * @return LLM 生成的产品描述
     */
    public String generateProductDescription(String product, String category, String tone) {
        String template = """
                你是一位资深的产品文案专家。
                请为以下产品撰写一段{tone}风格的产品描述：
                
                产品名称：{product}
                产品类别：{category}
                
                要求：
                1. 突出产品核心卖点
                2. 语言风格要{tone}
                3. 控制在 200 字以内
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(Map.of(
                "product", product,
                "category", category,
                "tone", tone
        ));

        log.info("PromptTemplate 产品描述生成，product={}, category={}, tone={}", product, category, tone);
        return chatClient.prompt(prompt).call().content();
    }

    /**
     * 基于模板进行代码翻译（解释）
     * <p>
     * 模板变量：{code}、{language}、{level}
     * </p>
     *
     * @param code     代码片段
     * @param language 编程语言
     * @param level    解释深度（如"初学者"、"中级"、"高级"）
     * @return LLM 对代码的解释
     */
    public String explainCode(String code, String language, String level) {
        String template = """
                你是一位经验丰富的{language}开发导师。
                请以适合{level}开发者理解的方式，解释以下{language}代码：
                
                ```{language}
                {code}
                ```
                
                请从以下几个方面进行解释：
                1. 代码的整体功能
                2. 关键语法和 API 的用法
                3. 可能的优化建议
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(Map.of(
                "code", code,
                "language", language,
                "level", level
        ));

        log.info("PromptTemplate 代码解释，language={}, level={}", language, level);
        return chatClient.prompt(prompt).call().content();
    }

    /**
     * 自定义模板对话（通用入口）
     * <p>
     * 允许调用方传入任意模板和变量，演示 PromptTemplate 的通用能力。
     * </p>
     *
     * @param template  提示词模板（使用 {key} 占位符）
     * @param variables 模板变量键值对
     * @return LLM 基于渲染后模板的回答
     */
    public String customTemplate(String template, Map<String, Object> variables) {
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(variables);

        log.info("PromptTemplate 自定义模板，variables={}", variables.keySet());
        return chatClient.prompt(prompt).call().content();
    }
}
