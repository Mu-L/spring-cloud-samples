package org.hongxi.cloud.sample.ai.service;

import org.hongxi.cloud.sample.ai.vo.PersonInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final ChatClient chatClient;

    public AiChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 简单聊天
     */
    public String chat(String message) {
        log.info("收到聊天请求: {}", message);
        
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 流式聊天（SSE）
     */
    public Flux<String> chatStream(String message) {
        log.info("开始流式对话: {}", message);

        // 使用 stream() 方法，返回 Flux 流式数据
        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .doOnNext(chunk -> log.debug("收到 chunk: {}", chunk))
                .doOnComplete(() -> log.info("流式对话完成"));
    }

    /**
     * 结构化输出 - 提取人员信息
     * eg. 我叫张三，今年25岁，是一名软件工程师，喜欢编程和打篮球，邮箱是zhangsan@example.com
     */
    public PersonInfo extractPersonInfo(String text) {
        log.info("提取人员信息: {}", text);
        
        String prompt = """
                从以下文本中提取人员信息，并以 JSON 格式返回：
                %s
                
                需要提取的字段：name(姓名), age(年龄), email(邮箱), occupation(职业)
                如果某个字段无法提取，请设置为 null。
                """.formatted(text);

        return chatClient.prompt(new Prompt(prompt))
                .call()
                .entity(PersonInfo.class);
    }
}
