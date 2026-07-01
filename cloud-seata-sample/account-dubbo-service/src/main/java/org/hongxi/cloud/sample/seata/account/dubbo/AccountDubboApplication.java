package org.hongxi.cloud.sample.seata.account.dubbo;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.hongxi.cloud.sample.api.CloudConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.apache.dubbo.common.constants.CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME;

@SpringBootApplication(exclude = {
        org.apache.dubbo.spring.boot.autoconfigure.observability.otel.OpenTelemetryAutoConfiguration.class
})
@EnableDubbo
public class AccountDubboApplication {

    public static void main(String[] args) {
        System.setProperty(DUBBO_PREFER_JSON_FRAMEWORK_NAME, CloudConstants.FASTJSON2);
        SpringApplication.run(AccountDubboApplication.class, args);
    }
}
