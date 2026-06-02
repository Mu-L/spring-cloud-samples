package org.hongxi.cloud.sample.nacos.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by javahongxi on 2026/6/1.
 */
@Data
@ConfigurationProperties(prefix = "cloud.agent")
@Component
public class AgentProperties {

    private String name;

    private String version;

    private int credits;

    private boolean enabled;

    private Provider provider = new Provider();

    @Data
    public static class Provider {

        private String name;

        private String model;

        private String apiKey;
    }
}
