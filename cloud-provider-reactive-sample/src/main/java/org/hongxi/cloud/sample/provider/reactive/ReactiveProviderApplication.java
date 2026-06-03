package org.hongxi.cloud.sample.provider.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import reactor.core.publisher.Hooks;

/**
 * Created by javahongxi on 2026/6/1.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ReactiveProviderApplication {
    public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(ReactiveProviderApplication.class, args);
    }
}
