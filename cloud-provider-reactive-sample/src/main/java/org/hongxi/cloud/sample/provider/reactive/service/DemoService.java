package org.hongxi.cloud.sample.provider.reactive.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Created by javahongxi on 2026/6/1.
 */
@RestController
public class DemoService {
    @Value("${server.port}")
    private String port;

    @RequestMapping("/hello")
    public Mono<String> hello(String name) {
        return Mono.just("Hi, " + name + ", Here is " + port);
    }
}
