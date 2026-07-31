package org.hongxi.cloud.sample.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@SpringBootApplication(excludeName = {
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration"
})
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
